package com.ailyrx.aichat.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * DeepSeek/OpenAI 兼容协议的流式客户端。
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);
    private static final int ERROR_BODY_LIMIT = 512;

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LlmClient(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 读取上游 SSE 增量并逐段回调，遇到 [DONE] 或客户端取消时结束。
     */
    public void streamChat(
            String systemPrompt,
            String userMessage,
            Consumer<String> onDelta,
            BooleanSupplier cancelled,
            String traceId) {
        ensureApiKey();

        HttpRequest request;
        try {
            request = buildRequest(systemPrompt, userMessage, true);
        } catch (IOException exception) {
            throw new LlmException("构造模型请求失败", exception);
        }

        try {
            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream body = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    String errorBody = readLimited(body);
                    log.warn("upstream model error traceId={} status={} body={}",
                            traceId, response.statusCode(), errorBody);
                    throw new LlmException("上游模型返回 HTTP " + response.statusCode());
                }

                consume(body, onDelta, cancelled);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmException("模型调用被中断", exception);
        } catch (IOException exception) {
            throw new LlmException("模型调用失败", exception);
        }
    }

    /**
     * 非流式调用，供结构化抽取使用。
     */
    public String complete(String systemPrompt, String userMessage, String traceId) {
        ensureApiKey();

        HttpRequest request;
        try {
            request = buildRequest(systemPrompt, userMessage, false);
        } catch (IOException exception) {
            throw new LlmException("构造模型请求失败", exception);
        }

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("upstream model error traceId={} status={} body={}",
                        traceId, response.statusCode(), truncate(response.body()));
                throw new LlmException("上游模型返回 HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root == null) {
                throw new LlmException("模型返回内容为空");
            }
            if (root.has("error")) {
                throw new LlmException("上游模型返回错误响应");
            }

            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new LlmException("模型返回内容为空");
            }
            return content.asText();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmException("模型调用被中断", exception);
        } catch (IOException exception) {
            throw new LlmException("模型调用失败", exception);
        }
    }

    private void ensureApiKey() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new LlmException("LLM_API_KEY 未配置");
        }
    }

    private HttpRequest buildRequest(String systemPrompt, String userMessage, boolean stream) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", properties.model());
        payload.put("stream", stream);

        ArrayNode messages = payload.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", systemPrompt);
        messages.addObject()
                .put("role", "user")
                .put("content", userMessage);

        int timeoutSeconds = properties.timeoutSeconds() > 0 ? properties.timeoutSeconds() : 60;

        return HttpRequest.newBuilder(buildEndpoint())
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", stream ? "text/event-stream" : "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(payload)))
                .build();
    }

    private URI buildEndpoint() {
        String baseUrl = properties.baseUrl() == null ? "" : properties.baseUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return URI.create(baseUrl + "/chat/completions");
    }

    private void consume(
            InputStream body,
            Consumer<String> onDelta,
            BooleanSupplier cancelled) throws IOException {
        boolean completed = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (cancelled.getAsBoolean()) {
                    return;
                }
                if (line.isBlank() || line.startsWith(":")) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }

                String data = line.substring("data:".length()).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    completed = true;
                    break;
                }

                JsonNode root = objectMapper.readTree(data);
                if (root.has("error")) {
                    throw new LlmException("上游模型返回错误响应");
                }

                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    continue;
                }

                JsonNode content = choices.path(0).path("delta").path("content");
                if (content.isTextual() && !content.asText().isEmpty()) {
                    onDelta.accept(content.asText());
                }
            }
        }

        if (!completed && !cancelled.getAsBoolean()) {
            throw new LlmException("模型响应提前结束");
        }
    }

    private String readLimited(InputStream body) {
        try {
            byte[] bytes = body.readNBytes(ERROR_BODY_LIMIT);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        String normalized = body.strip();
        return normalized.length() <= ERROR_BODY_LIMIT
                ? normalized
                : normalized.substring(0, ERROR_BODY_LIMIT) + "...";
    }
}

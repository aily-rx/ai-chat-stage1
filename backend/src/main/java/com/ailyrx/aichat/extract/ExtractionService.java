package com.ailyrx.aichat.extract;

import com.ailyrx.aichat.llm.LlmClient;
import com.ailyrx.aichat.prompt.PromptService;
import com.ailyrx.aichat.prompt.PromptTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);
    private static final int MAX_RAW_LOG_LENGTH = 2000;
    private static final int MAX_ATTEMPTS = 2;
    private static final DateTimeFormatter ISO_DATE =
            DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern CHINESE_DATE =
            Pattern.compile("([0-9]{4})年([0-9]{1,2})月([0-9]{1,2})日");

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    public ExtractionService(LlmClient llmClient, ObjectMapper objectMapper, PromptService promptService) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.promptService = promptService;
    }

    public ExtractResult extract(String text, String traceId) {
        PromptTemplate prompt = promptService.extractPrompt();
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt += 1) {
            String raw = llmClient.complete(prompt.content(), text, traceId);
            try {
                ExtractResult result = parse(raw, traceId);
                if (result != null) {
                    log.info("extract completed traceId={} promptId={} promptVersion={}",
                            traceId, prompt.id(), prompt.version());
                    return result;
                }
                log.warn("extract output invalid traceId={} attempt={} promptId={} promptVersion={} raw={}",
                        traceId, attempt, prompt.id(), prompt.version(), truncate(raw));
            } catch (Exception exception) {
                log.warn("extract output parse failed traceId={} attempt={} promptId={} promptVersion={} raw={}",
                        traceId, attempt, prompt.id(), prompt.version(), truncate(raw), exception);
            }
        }

        throw new ExtractionParseException("模型输出无法解析，已重试 1 次");
    }

    private ExtractResult parse(String raw, String traceId) throws Exception {
        String json = stripFences(raw);
        if (json.isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(json);
        if (root == null || !root.isObject() || !containsExtractField(root)) {
            return null;
        }

        return new ExtractResult(
                textOrNull(root.get("docType")),
                textOrNull(root.get("title")),
                textOrNull(root.get("recipient")),
                textOrNull(root.get("signature")),
                normalizeDate(textOrNull(root.get("date")), traceId));
    }

    private boolean containsExtractField(JsonNode root) {
        return root.has("docType")
                || root.has("title")
                || root.has("recipient")
                || root.has("signature")
                || root.has("date");
    }

    private String stripFences(String raw) {
        if (raw == null) {
            return "";
        }

        String cleaned = raw.strip();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf(10);
            if (firstNewline >= 0) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
            int closingFence = cleaned.lastIndexOf("```");
            if (closingFence >= 0) {
                cleaned = cleaned.substring(0, closingFence);
            }
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        return cleaned.strip();
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText().strip();
        return value.isEmpty() ? null : value;
    }

    private String normalizeDate(String value, String traceId) {
        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value, ISO_DATE).format(ISO_DATE);
        } catch (DateTimeParseException ignored) {
            // 继续尝试常见中文日期格式。
        }

        Matcher matcher = CHINESE_DATE.matcher(value);
        if (matcher.matches()) {
            try {
                LocalDate date = LocalDate.of(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)));
                return date.format(ISO_DATE);
            } catch (DateTimeException ignored) {
                // 非法日期按无法识别处理。
            }
        }

        log.warn("extract date invalid traceId={} value={}", traceId, value);
        return null;
    }

    private String truncate(String raw) {
        if (raw == null) {
            return "";
        }
        String stripped = raw.strip();
        return stripped.length() <= MAX_RAW_LOG_LENGTH
                ? stripped
                : stripped.substring(0, MAX_RAW_LOG_LENGTH) + "...";
    }
}

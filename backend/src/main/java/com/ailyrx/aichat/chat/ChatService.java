package com.ailyrx.aichat.chat;

import com.ailyrx.aichat.common.ErrorCodes;
import com.ailyrx.aichat.llm.LlmClient;
import com.ailyrx.aichat.llm.LlmOutputTruncatedException;
import com.ailyrx.aichat.prompt.PromptService;
import com.ailyrx.aichat.prompt.PromptTemplate;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final PromptService promptService;
    private final LlmClient llmClient;

    public ChatService(PromptService promptService, LlmClient llmClient) {
        this.promptService = promptService;
        this.llmClient = llmClient;
    }

    public void stream(
            String message,
            String style,
            String traceId,
            SseEmitter emitter,
            AtomicBoolean closed) {
        AtomicInteger characterCount = new AtomicInteger();

        try {
            PromptTemplate prompt = promptService.chatPrompt(style);
            llmClient.streamChat(
                    prompt.content(),
                    message,
                    delta -> {
                        characterCount.addAndGet(delta.length());
                        sendDelta(emitter, delta, closed);
                    },
                    closed::get,
                    traceId);

            if (closed.get()) {
                return;
            }

            log.info("chat stream completed traceId={} chars={} promptId={} promptVersion={}",
                    traceId, characterCount.get(), prompt.id(), prompt.version());
            sendEvent(emitter, SseEmitter.event()
                    .name("done")
                    .data(Map.of("traceId", traceId)), closed);
            safeComplete(emitter);
        } catch (SseSendException exception) {
            log.debug("client disconnected traceId={}", traceId);
            safeComplete(emitter);
        } catch (LlmOutputTruncatedException exception) {
            if (closed.get()) {
                log.debug("stream already closed traceId={}", traceId);
                safeComplete(emitter);
                return;
            }

            log.warn("chat stream truncated traceId={} reason={}", traceId, exception.getMessage());
            sendError(emitter, traceId, "模型输出被截断，请缩短问题或重试");
            safeComplete(emitter);
        } catch (Exception exception) {
            if (closed.get()) {
                log.debug("stream already closed traceId={}", traceId);
                safeComplete(emitter);
                return;
            }

            log.error("chat stream failed traceId={}", traceId, exception);
            sendError(emitter, traceId, "模型调用失败");
            safeComplete(emitter);
        }
    }

    private void sendDelta(SseEmitter emitter, String delta, AtomicBoolean closed) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        sendEvent(emitter, SseEmitter.event()
                .name("message")
                .data(Map.of("delta", delta)), closed);
    }

    private void sendEvent(SseEmitter emitter, SseEmitter.SseEventBuilder event, AtomicBoolean closed) {
        if (closed.get()) {
            throw new SseSendException("client disconnected", null);
        }
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            throw new SseSendException("failed to write SSE event", exception);
        }
    }

    private void sendError(SseEmitter emitter, String traceId, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of(
                            "code", ErrorCodes.MODEL_CALL_FAILED,
                            "message", message,
                            "traceId", traceId)));
        } catch (IOException | IllegalStateException exception) {
            log.debug("failed to send SSE error traceId={}", traceId);
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // 连接已关闭时无需再次处理。
        }
    }

    private static final class SseSendException extends RuntimeException {

        private SseSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

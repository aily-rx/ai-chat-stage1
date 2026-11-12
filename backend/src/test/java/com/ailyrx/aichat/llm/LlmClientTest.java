package com.ailyrx.aichat.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LlmClientTest {

    private LlmClient client;

    @BeforeEach
    void setUp() {
        client = new LlmClient(
                new LlmProperties("http://localhost:1", "test-key", "deepseek-chat", 5, 0.0),
                new ObjectMapper());
    }

    @Test
    void detectsLengthFinishReasonInStream() {
        String stream = """
                data: {"choices":[{"delta":{"content":"hello"},"finish_reason":null}]}

                data: {"choices":[{"delta":{},"finish_reason":"length"}]}

                data: [DONE]

                """;

        assertThatThrownBy(() -> client.consume(
                        new ByteArrayInputStream(stream.getBytes(StandardCharsets.UTF_8)),
                        delta -> { },
                        () -> false))
                .isInstanceOf(LlmOutputTruncatedException.class)
                .hasMessageContaining("长度上限");
    }

    @Test
    void acceptsStopFinishReasonWithoutDoneMarker() throws Exception {
        String stream = """
                data: {"choices":[{"delta":{"content":"hello"},"finish_reason":null}]}

                data: {"choices":[{"delta":{},"finish_reason":"stop"}]}

                """;
        List<String> deltas = new ArrayList<>();

        client.consume(
                new ByteArrayInputStream(stream.getBytes(StandardCharsets.UTF_8)),
                deltas::add,
                () -> false);

        assertThat(deltas).containsExactly("hello");
    }

    @Test
    void rejectsUnexpectedFinishReason() {
        String stream = """
                data: {"choices":[{"delta":{},"finish_reason":"content_filter"}]}

                data: [DONE]

                """;

        assertThatThrownBy(() -> client.consume(
                        new ByteArrayInputStream(stream.getBytes(StandardCharsets.UTF_8)),
                        delta -> { },
                        () -> false))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("content_filter");
    }

    @Test
    void addsTemperatureOnlyForExtractionPayload() {
        ObjectNode chatPayload = client.buildPayload("system", "user", true, null);
        ObjectNode extractPayload = client.buildPayload("system", "user", false, 0.0);

        assertThat(chatPayload.path("stream").asBoolean()).isTrue();
        assertThat(chatPayload.has("temperature")).isFalse();
        assertThat(extractPayload.path("stream").asBoolean()).isFalse();
        assertThat(extractPayload.path("temperature").asDouble()).isEqualTo(0.0);
        assertThat(extractPayload.path("messages").size()).isEqualTo(2);
    }
}

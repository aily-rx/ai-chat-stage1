package com.ailyrx.aichat.extract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ailyrx.aichat.llm.LlmClient;
import com.ailyrx.aichat.llm.LlmException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtractionServiceTest {

    private static final String TRACE_ID = "trace-extract";

    @Mock
    private LlmClient llmClient;

    private ExtractionService extractionService;

    @BeforeEach
    void setUp() {
        extractionService = new ExtractionService(llmClient, new ObjectMapper());
    }

    @Test
    void parsesAllFields() {
        when(llmClient.complete(anyString(), anyString(), eq(TRACE_ID))).thenReturn("""
                {"docType":"通知","title":"关于开展安全生产检查的通知",
                 "recipient":"各部门","signature":"XX办公室","date":"2026-11-01"}
                """);

        ExtractResult result = extractionService.extract("公文正文", TRACE_ID);

        assertThat(result.docType()).isEqualTo("通知");
        assertThat(result.title()).isEqualTo("关于开展安全生产检查的通知");
        assertThat(result.recipient()).isEqualTo("各部门");
        assertThat(result.signature()).isEqualTo("XX办公室");
        assertThat(result.date()).isEqualTo("2026-11-01");
    }

    @Test
    void stripsMarkdownFenceAndNormalizesChineseDate() {
        when(llmClient.complete(anyString(), anyString(), eq(TRACE_ID))).thenReturn("""
                ```json
                {"docType":"通知","title":"测试通知","recipient":null,
                 "signature":"XX办公室","date":"2026年11月1日"}
                ```
                """);

        ExtractResult result = extractionService.extract("公文正文", TRACE_ID);

        assertThat(result.title()).isEqualTo("测试通知");
        assertThat(result.recipient()).isNull();
        assertThat(result.date()).isEqualTo("2026-11-01");
    }

    @Test
    void fillsMissingFieldsWithNull() {
        when(llmClient.complete(anyString(), anyString(), eq(TRACE_ID)))
                .thenReturn("{\"title\":\"只有标题\"}");

        ExtractResult result = extractionService.extract("公文正文", TRACE_ID);

        assertThat(result.title()).isEqualTo("只有标题");
        assertThat(result.docType()).isNull();
        assertThat(result.recipient()).isNull();
        assertThat(result.signature()).isNull();
        assertThat(result.date()).isNull();
    }

    @Test
    void acceptsAllNullFields() {
        when(llmClient.complete(anyString(), anyString(), eq(TRACE_ID))).thenReturn(
                "{\"docType\":null,\"title\":null,\"recipient\":null,"
                        + "\"signature\":null,\"date\":null}");

        ExtractResult result = extractionService.extract("无法识别的文本", TRACE_ID);

        assertThat(result.docType()).isNull();
        assertThat(result.title()).isNull();
        assertThat(result.recipient()).isNull();
        assertThat(result.signature()).isNull();
        assertThat(result.date()).isNull();
        verify(llmClient, times(1)).complete(anyString(), anyString(), eq(TRACE_ID));
    }

    @Test
    void emptyJsonObjectTriggersRetryAndFails() {
        when(llmClient.complete(anyString(), anyString(), eq(TRACE_ID)))
                .thenReturn("{}", "{}");

        assertThatThrownBy(() -> extractionService.extract("公文正文", TRACE_ID))
                .isInstanceOf(ExtractionParseException.class);
        verify(llmClient, times(2)).complete(anyString(), anyString(), eq(TRACE_ID));
    }

    @Test
    void invalidDateBecomesNull() {
        when(llmClient.complete(anyString(), anyString(), eq(TRACE_ID))).thenReturn("""
                {"docType":"通知","title":"测试通知","recipient":"各部门",
                 "signature":"XX办公室","date":"2026-02-31"}
                """);

        ExtractResult result = extractionService.extract("公文正文", TRACE_ID);

        assertThat(result.date()).isNull();
    }

    @Test
    void retriesOnceAndSucceeds() {
        when(llmClient.complete(anyString(), anyString(), eq(TRACE_ID)))
                .thenReturn("not json", "{\"title\":\"重试成功\"}");

        ExtractResult result = extractionService.extract("公文正文", TRACE_ID);

        assertThat(result.title()).isEqualTo("重试成功");
        verify(llmClient, times(2)).complete(anyString(), anyString(), eq(TRACE_ID));
    }

    @Test
    void failsWithParseExceptionAfterTwoInvalidOutputs() {
        when(llmClient.complete(anyString(), anyString(), eq(TRACE_ID)))
                .thenReturn("not json", "still not json");

        assertThatThrownBy(() -> extractionService.extract("公文正文", TRACE_ID))
                .isInstanceOf(ExtractionParseException.class)
                .hasMessage("模型输出无法解析，已重试 1 次");
        verify(llmClient, times(2)).complete(anyString(), anyString(), eq(TRACE_ID));
    }

    @Test
    void propagatesModelCallFailureWithoutRetry() {
        when(llmClient.complete(anyString(), anyString(), eq(TRACE_ID)))
                .thenThrow(new LlmException("upstream failure"));

        assertThatThrownBy(() -> extractionService.extract("公文正文", TRACE_ID))
                .isInstanceOf(LlmException.class);
        verify(llmClient, times(1)).complete(anyString(), anyString(), eq(TRACE_ID));
    }
}

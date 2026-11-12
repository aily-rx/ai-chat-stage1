package com.ailyrx.aichat.extract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ailyrx.aichat.llm.LlmClient;
import com.ailyrx.aichat.llm.LlmException;
import com.ailyrx.aichat.llm.LlmOutputTruncatedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ExtractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmClient llmClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsFixedFields() throws Exception {
        when(llmClient.complete(anyString(), anyString(), anyString())).thenReturn("""
                {"docType":"通知","title":"关于开展安全生产检查的通知",
                 "recipient":"各部门","signature":"XX办公室","date":"2026-11-01"}
                """);

        MvcResult result = mockMvc.perform(post("/api/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"test document\"}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn();

        JsonNode root = readBody(result);
        assertThat(root.path("code").asInt()).isZero();
        assertThat(root.path("data").path("docType").asText()).isEqualTo("通知");
        assertThat(root.path("data").path("title").asText()).isEqualTo("关于开展安全生产检查的通知");
        assertThat(root.path("data").path("date").asText()).isEqualTo("2026-11-01");
        assertThat(root.path("data").has("signature")).isTrue();
    }

    @Test
    void returns5003AfterRetryWithoutHttpError() throws Exception {
        when(llmClient.complete(anyString(), anyString(), anyString()))
                .thenReturn("not json", "still not json");

        MvcResult result = mockMvc.perform(post("/api/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"test document\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = readBody(result);
        assertThat(root.path("code").asInt()).isEqualTo(5003);
        assertThat(root.path("message").asText()).isEqualTo("模型输出无法解析，已重试 1 次");
        assertThat(root.path("traceId").asText()).isNotBlank();
    }

    @Test
    void returns5001WhenModelCallFails() throws Exception {
        when(llmClient.complete(anyString(), anyString(), anyString()))
                .thenThrow(new LlmException("upstream failure"));

        MvcResult result = mockMvc.perform(post("/api/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"test document\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = readBody(result);
        assertThat(root.path("code").asInt()).isEqualTo(5001);
        assertThat(root.path("message").asText()).isEqualTo("模型调用失败");
    }

    @Test
    void returns5001WhenModelOutputIsTruncated() throws Exception {
        when(llmClient.complete(anyString(), anyString(), anyString()))
                .thenThrow(new LlmOutputTruncatedException("模型输出达到长度上限"));

        MvcResult result = mockMvc.perform(post("/api/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"test document\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = readBody(result);
        assertThat(root.path("code").asInt()).isEqualTo(5001);
        assertThat(root.path("message").asText()).isEqualTo("模型输出被截断，请缩短输入或重试");
    }

    @Test
    void rejectsBlankText() throws Exception {
        mockMvc.perform(post("/api/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("text 不能为空"));
    }

    @Test
    void rejectsOverlongText() throws Exception {
        String payload = objectMapper.createObjectNode()
                .put("text", "安".repeat(10001))
                .toString();

        mockMvc.perform(post("/api/extract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("text 不能超过 10000 字符"));
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

package com.ailyrx.aichat.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ailyrx.aichat.llm.LlmClient;
import com.ailyrx.aichat.llm.LlmException;
import com.ailyrx.aichat.llm.LlmOutputTruncatedException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
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
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmClient llmClient;

    @Test
    void streamsDeltasAndDoneEvent() throws Exception {
        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(2);
            onDelta.accept("你好");
            onDelta.accept("，世界");
            return null;
        }).when(llmClient).streamChat(anyString(), anyString(), any(), any(), anyString());

        MvcResult result = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"写一句问候\",\"style\":\"formal\"}"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult dispatched = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(header().string("Cache-Control", "no-cache, no-transform"))
                .andExpect(header().string("X-Accel-Buffering", "no"))
                .andReturn();

        String body = dispatched.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertThat(body)
                .contains("event:message")
                .contains("\"delta\":\"你好\"")
                .contains("\"delta\":\"，世界\"")
                .contains("event:done")
                .contains("\"traceId\":");
    }

    @Test
    void sendsErrorEventWhenModelFails() throws Exception {
        doThrow(new LlmException("upstream failure"))
                .when(llmClient).streamChat(anyString(), anyString(), any(), any(), anyString());

        MvcResult result = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"写通知\",\"style\":\"concise\"}"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult dispatched = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn();

        String body = dispatched.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertThat(body)
                .contains("event:error")
                .contains("\"code\":5001")
                .contains("模型调用失败");
    }

    @Test
    void sendsTruncationErrorEventWhenOutputHitsLengthLimit() throws Exception {
        doThrow(new LlmOutputTruncatedException("模型输出达到长度上限"))
                .when(llmClient).streamChat(anyString(), anyString(), any(), any(), anyString());

        MvcResult result = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"写一份长通知\",\"style\":\"formal\"}"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult dispatched = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn();

        String body = dispatched.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Assertions.assertThat(body)
                .contains("event:error")
                .contains("\"code\":5001")
                .contains("模型输出被截断")
                .doesNotContain("event:done");
    }

    @Test
    void allowsDevelopmentOrigin() throws Exception {
        mockMvc.perform(options("/api/chat/stream")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Trace-Id"));
    }

    @Test
    void rejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/chat/stream")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \",\"style\":\"formal\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("message 不能为空"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void rejectsUnsupportedStyle() throws Exception {
        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"写通知\",\"style\":\"poetic\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("style 只支持 formal 或 concise"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }
}

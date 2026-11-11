package com.ailyrx.aichat.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReturnsOkWithGeneratedTraceId() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data").value("ok"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void healthReusesIncomingTraceId() throws Exception {
        mockMvc.perform(get("/api/health").header("X-Trace-Id", "test-trace-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "test-trace-123"))
                .andExpect(jsonPath("$.traceId").value("test-trace-123"));
    }
}

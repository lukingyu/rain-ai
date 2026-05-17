package com.rain.ai.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.ai.openai.chat.api-key=test-key",
                "spring.ai.openai.embedding.api-key=test-key"
        }
)
@AutoConfigureMockMvc
class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 健康检查返回应用状态() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").value("rain-ai"));
    }

    @Test
    void AI配置体检不暴露密钥() throws Exception {
        mockMvc.perform(get("/api/health/ai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chat.configured").value(false))
                .andExpect(jsonPath("$.data.embedding.configured").value(false))
                .andExpect(jsonPath("$.data.chat.provider").value("openai-compatible-chat"))
                .andExpect(jsonPath("$.data.embedding.provider").value("openai-compatible-embedding"));
    }

    @Test
    void AI在线探针在未配置密钥时跳过真实调用() throws Exception {
        mockMvc.perform(post("/api/health/ai/probe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chat.skipped").value(true))
                .andExpect(jsonPath("$.data.embedding.skipped").value(true));
    }
}

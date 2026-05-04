package com.redis.workshop.springai.fundamentals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=",
        "spring.ai.model.chat=none"
})
@AutoConfigureMockMvc
class SpringAiFundamentalsApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void statusReportsModelAndMissingKey() throws Exception {
        mockMvc.perform(get("/api/spring-ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKeyConfigured", is(false)))
                .andExpect(jsonPath("$.model", is("gpt-4o-mini")));
    }

    @Test
    void promptReturnsClearErrorWhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(post("/api/spring-ai/prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error", containsString("OpenAI API key is not configured")));
    }

    @Test
    void toolsReturnSyntheticSnapshotWhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(post("/api/spring-ai/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"MSFT\",\"question\":\"What changed?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.toolInvoked", is(false)))
                .andExpect(jsonPath("$.snapshot.ticker", is("MSFT")));
    }
}

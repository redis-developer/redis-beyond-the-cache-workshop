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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
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
    void runnerReadinessPathReportsReady() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ready")));
    }

    @Test
    void learnerAppIsServedFromAppPath() throws Exception {
        mockMvc.perform(get("/app/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/app/index.html"));

        mockMvc.perform(get("/app/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Stock Analysis Helper")));
    }

    @Test
    void promptReturnsStageZeroPlaceholder() throws Exception {
        mockMvc.perform(post("/api/spring-ai/prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.answer", containsString("Spring AI is not wired yet")));
    }

    @Test
    void toolsAreStillScaffoldedAtStageZero() throws Exception {
        mockMvc.perform(post("/api/spring-ai/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"MSFT\",\"question\":\"What changed?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.toolInvoked", is(false)))
                .andExpect(jsonPath("$.error", containsString("not implemented yet")));
    }
}

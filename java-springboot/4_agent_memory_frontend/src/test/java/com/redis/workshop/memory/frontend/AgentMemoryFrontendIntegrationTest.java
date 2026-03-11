package com.redis.workshop.memory.frontend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = AgentMemoryFrontendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "workshop.backend.url=http://127.0.0.1:1"
)
@AutoConfigureMockMvc
class AgentMemoryFrontendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void editorApiIsExposedInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/editor/files"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasKey("files")))
            .andExpect(jsonPath("$", hasKey("workshopTitle")))
            .andExpect(jsonPath("$.workshopTitle").value("Agent Memory Workshop"))
            .andExpect(jsonPath("$.files.length()").value(3))
            .andExpect(jsonPath(
                "$.files[*].name",
                containsInAnyOrder("AgentMemoryService.java", "AmsChatMemoryRepository.java", "ChatService.java")
            ));
    }

    @Test
    void memoryApiIsHandledByProxyInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/memory/health"))
            .andExpect(status().isBadGateway());
    }

    @Test
    void contentApiExposesWorkshopOwnedViews() throws Exception {
        mockMvc.perform(get("/api/content/manifest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workshopId").value("4_agent_memory"))
            .andExpect(jsonPath("$.views", hasSize(6)));

        mockMvc.perform(get("/api/content/views/memory-home"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewId").value("memory-home"))
            .andExpect(jsonPath("$.pageType").value("stage-flow"));

        mockMvc.perform(get("/api/content/views/memory-editor"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewId").value("memory-editor"))
            .andExpect(jsonPath("$.pageType").value("editor"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/challenges", "/demo", "/learn", "/editor", "/lab"})
    void spaRoutesResolveToFrontend(String route) throws Exception {
        mockMvc.perform(get(route))
            .andExpect(status().isOk());
    }
}

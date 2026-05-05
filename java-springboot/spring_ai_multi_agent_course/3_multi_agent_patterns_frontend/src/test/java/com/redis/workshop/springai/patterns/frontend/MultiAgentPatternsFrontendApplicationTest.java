package com.redis.workshop.springai.patterns.frontend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = MultiAgentPatternsFrontendApplication.class,
    properties = "workshop.backend.url=http://127.0.0.1:1"
)
@AutoConfigureMockMvc
class MultiAgentPatternsFrontendApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void contentManifestExposesModulePages() throws Exception {
        mockMvc.perform(get("/api/content/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workshopId").value("3_multi_agent_patterns"))
                .andExpect(jsonPath("$.views", hasSize(7)));

        mockMvc.perform(get("/api/content/views/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").value("/6"))
                .andExpect(jsonPath("$.pageType").value("editor"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "2", "3", "4", "5", "6"})
    void allWorkshopContentViewsLoad(String viewId) throws Exception {
        mockMvc.perform(get("/api/content/views/{viewId}", viewId))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/0", "/1", "/2", "/3", "/4", "/5", "/6", "/editor"})
    void spaRoutesResolveToFrontend(String route) throws Exception {
        mockMvc.perform(get(route))
                .andExpect(status().isOk());
    }
}

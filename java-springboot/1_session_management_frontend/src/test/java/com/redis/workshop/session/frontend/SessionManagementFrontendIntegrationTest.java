package com.redis.workshop.session.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = SessionManagementFrontendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "workshop.backend.url=http://127.0.0.1:1"
)
@AutoConfigureMockMvc
class SessionManagementFrontendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void editorApiIsExposedInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/editor/files"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasKey("files")))
            .andExpect(jsonPath("$", hasKey("workshopTitle")))
            .andExpect(jsonPath("$.workshopTitle").value("Distributed Session Management with Redis"))
            .andExpect(jsonPath("$.files[*].name", hasItem("SecurityConfig.java")));
    }

    @Test
    void spaRoutesAreHandledByFrontendModule() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/welcome"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/editor"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void sessionApiIsHandledByProxyInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/session-info"))
            .andExpect(status().isBadGateway());
    }

    @Test
    void sharedContentApiIsExposedInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/content/manifest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workshopId").value("1_session_management"))
            .andExpect(jsonPath("$.views[*].viewId", hasItem("session-home")))
            .andExpect(jsonPath("$.views[*].viewId", hasItem("session-editor")));

        mockMvc.perform(get("/api/content/views/session-home"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageType").value("stage-flow"))
            .andExpect(jsonPath("$.stages[0].sections[0].blocks[2].listId").value("stage1-tests"));

        mockMvc.perform(get("/api/content/views/session-editor"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageType").value("editor"))
            .andExpect(jsonPath("$.sections[1].blocks[0].type").value("editorStepList"));
    }

    @Test
    void loginPostIsHandledByAuthProxyInFrontendModule() throws Exception {
        mockMvc.perform(post("/login").param("username", "user").param("password", "password"))
            .andExpect(status().isBadGateway());
    }
}

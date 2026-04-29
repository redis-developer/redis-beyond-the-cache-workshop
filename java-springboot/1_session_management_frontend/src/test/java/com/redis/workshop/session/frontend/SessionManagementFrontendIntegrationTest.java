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
            .andExpect(redirectedUrl("/0"));

        mockMvc.perform(get("/0"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/1"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/2"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/3"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/4"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/redis-insight-view"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/welcome"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/0"));

        mockMvc.perform(get("/editor"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/4"));
    }

    @Test
    void sessionApiIsHandledByProxyInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/session-info"))
            .andExpect(status().isBadGateway());
    }

    @Test
    void learnerAppIsHandledByBackendProxyInFrontendModule() throws Exception {
        mockMvc.perform(get("/app/"))
            .andExpect(status().isBadGateway());
    }

    @Test
    void sharedContentApiIsExposedInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/content/manifest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workshopId").value("1_session_management"))
            .andExpect(jsonPath("$.views[*].viewId", hasItem("0")))
            .andExpect(jsonPath("$.views[*].viewId", hasItem("1")))
            .andExpect(jsonPath("$.views[*].viewId", hasItem("2")))
            .andExpect(jsonPath("$.views[*].viewId", hasItem("3")))
            .andExpect(jsonPath("$.views[*].viewId", hasItem("4")));

        mockMvc.perform(get("/api/content/views/0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageType").value("narrative"))
            .andExpect(jsonPath("$.title").value("STAGE 0: Sessions And State"))
            .andExpect(jsonPath("$.navigation.nextLabel").value("Experience the problem"));

        mockMvc.perform(get("/api/content/views/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageType").value("narrative"))
            .andExpect(jsonPath("$.navigation.previousLabel").value(""))
            .andExpect(jsonPath("$.navigation.nextLabel").value("Show me how fix it"))
            .andExpect(jsonPath("$.sections[0].blocks[2].listId").value("stage1-tests"));

        mockMvc.perform(get("/api/content/views/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageType").value("narrative"))
            .andExpect(jsonPath("$.title").value("STAGE 2: Enable Redis Session Management"))
            .andExpect(jsonPath("$.navigation.previousLabel").value(""))
            .andExpect(jsonPath("$.navigation.nextLabel").value("Experience distributed sessions"));

        mockMvc.perform(get("/api/content/views/3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageType").value("narrative"))
            .andExpect(jsonPath("$.sections[0].blocks[2].listId").value("stage3-tests"));

        mockMvc.perform(get("/api/content/views/4"))
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

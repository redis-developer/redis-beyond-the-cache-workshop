package com.redis.workshop.session.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
            .andExpect(jsonPath("$", hasKey("workshopTitle")));
    }

    @Test
    void sessionApiIsHandledByProxyInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/session-info"))
            .andExpect(status().isBadGateway());
    }

    @Test
    void loginPostIsHandledByAuthProxyInFrontendModule() throws Exception {
        mockMvc.perform(post("/login").param("username", "user").param("password", "password"))
            .andExpect(status().isBadGateway());
    }
}

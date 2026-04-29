package com.redis.workshop.session;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SessionManagementAppIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesLearnerAppFromLearnerRuntimePackage() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/app/"));

        mockMvc.perform(get("/app"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/app/index.html"));

        mockMvc.perform(get("/app/"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/app/index.html"));

        mockMvc.perform(get("/app/index.html"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
            .andExpect(content().string(containsString("Session Management App")));
    }

    @Test
    void sessionApiStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/session-info"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user")
    void authenticatedSessionApiIsAvailableToLearnerApp() throws Exception {
        mockMvc.perform(get("/api/session-info"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"username\":\"user\"")));
    }
}

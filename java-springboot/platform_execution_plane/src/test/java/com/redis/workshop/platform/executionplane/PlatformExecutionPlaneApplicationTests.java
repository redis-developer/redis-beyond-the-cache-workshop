package com.redis.workshop.platform.executionplane;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "platform.execution-plane.internal-api.shared-secret=test-secret")
@AutoConfigureMockMvc
class PlatformExecutionPlaneApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void internalApiRejectsMissingSharedSecret() throws Exception {
        mockMvc.perform(post("/internal/execution-plane/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void internalApiAcceptsSharedSecretBeforeValidation() throws Exception {
        mockMvc.perform(post("/internal/execution-plane/sessions")
                .header("X-Execution-Plane-Key", "test-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}

package com.redis.workshop.search;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = FullTextSearchApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
class FullTextSearchBackendModeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchApiIsServedByBackendMode() throws Exception {
        mockMvc.perform(get("/api/search"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasKey("movies")))
            .andExpect(jsonPath("$", hasKey("count")))
            .andExpect(jsonPath("$", hasKey("searchTime")));
    }

    @Test
    void editorApiIsNotExposedInBackendMode() throws Exception {
        mockMvc.perform(get("/api/editor/files"))
            .andExpect(status().isNotFound());
    }
}

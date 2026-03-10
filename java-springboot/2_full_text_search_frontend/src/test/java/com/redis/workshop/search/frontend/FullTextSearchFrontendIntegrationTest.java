package com.redis.workshop.search.frontend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = FullTextSearchFrontendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "workshop.backend.url=http://127.0.0.1:1"
)
@AutoConfigureMockMvc
class FullTextSearchFrontendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void frontendRuntimeProperties(DynamicPropertyRegistry registry) {
        registry.add("workshop.source.path", FullTextSearchFrontendIntegrationTest::resolveWorkshopSourcePath);
    }

    private static String resolveWorkshopSourcePath() {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        String directoryName = userDir.getFileName() != null ? userDir.getFileName().toString() : "";

        Path sourcePath = switch (directoryName) {
            case "2_full_text_search_frontend" -> userDir.resolveSibling("2_full_text_search");
            case "java-springboot" -> userDir.resolve("2_full_text_search");
            case "redis-beyond-the-cache-workshop" -> userDir.resolve("java-springboot").resolve("2_full_text_search");
            default -> userDir.resolve("java-springboot").resolve("2_full_text_search");
        };

        return sourcePath.toAbsolutePath().normalize().toString();
    }

    @Test
    void editorApiIsExposedInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/editor/files"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasKey("files")))
            .andExpect(jsonPath("$", hasKey("workshopTitle")))
            .andExpect(jsonPath("$", hasKey("workshopDescription")))
            .andExpect(jsonPath("$.workshopTitle").value("Full-Text Search with Redis"))
            .andExpect(jsonPath("$.files[*].name", hasItem("SearchService.java")));
    }

    @Test
    void editorApiLoadsBackendFilesFromManifestConfiguration() throws Exception {
        mockMvc.perform(get("/api/editor/file/Movie.java"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fileName").value("Movie.java"))
            .andExpect(jsonPath("$.content", containsString("private List<String> genres;")))
            .andExpect(jsonPath("$.content", containsString("private String extract;")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/search", "/editor"})
    void spaRoutesForwardToIndexHtml(String route) throws Exception {
        mockMvc.perform(get(route))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void searchApiIsHandledByProxyInFrontendModule() throws Exception {
        mockMvc.perform(get("/api/search"))
            .andExpect(status().isBadGateway());
    }
}

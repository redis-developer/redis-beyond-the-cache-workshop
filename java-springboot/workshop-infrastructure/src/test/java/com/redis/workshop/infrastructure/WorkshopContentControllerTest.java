package com.redis.workshop.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkshopContentControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void servesManifestAndViewsFromSharedContentApi() throws Exception {
        WorkshopContentController controller = new WorkshopContentController(contentLoader(writeWorkshopContent()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/content/manifest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workshopId").value("1_session_management"))
            .andExpect(jsonPath("$.views[0].viewId").value("session-home"));

        mockMvc.perform(get("/api/content/views/session-home"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewId").value("session-home"))
            .andExpect(jsonPath("$.pageType").value("narrative"))
            .andExpect(jsonPath("$.sections[0].blocks[0].type").value("statusPanel"))
            .andExpect(jsonPath("$.sections[0].blocks[0].actions[0].id").value("openEditor"));
    }

    @Test
    void returnsStructured404WhenContentManifestIsMissing() throws Exception {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSourcePath(tempDir.resolve("missing-module").toString());

        WorkshopContentLoader loader = new WorkshopContentLoader(
            new MockEnvironment(),
            new DefaultResourceLoader(),
            runtimeProperties,
            () -> java.util.Set.of()
        );

        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new WorkshopContentController(loader))
            .build();

        mockMvc.perform(get("/api/content/manifest"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("WORKSHOP_CONTENT_NOT_FOUND"));
    }

    private Path writeWorkshopContent() throws IOException {
        Path moduleRoot = Files.createDirectories(tempDir.resolve("search-frontend"));
        Path contentRoot = Files.createDirectories(moduleRoot.resolve("src/main/resources/workshop-content/views"));

        Files.writeString(
            contentRoot.getParent().resolve("manifest.yaml"),
            """
                schemaVersion: 1
                workshopId: 1_session_management
                views:
                  - viewId: session-home
                    route: /welcome
                    pageType: narrative
                    file: views/session-home.yaml
                """
        );

        Files.writeString(
            contentRoot.resolve("session-home.yaml"),
            """
                schemaVersion: 1
                viewId: session-home
                route: /welcome
                pageType: narrative
                title: Session Management
                slot: instructions
                sections:
                  - sectionId: overview
                    blocks:
                      - type: statusPanel
                        tone: info
                        title: Current State
                        body: Use the editor for the guided path.
                        actions:
                          - id: openEditor
                            label: Open Code Editor
                """
        );

        return moduleRoot;
    }

    private static WorkshopContentLoader contentLoader(Path moduleRoot) {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSourcePath(moduleRoot.toString());
        return new WorkshopContentLoader(
            new MockEnvironment(),
            new DefaultResourceLoader(),
            runtimeProperties,
            () -> java.util.Set.of()
        );
    }
}

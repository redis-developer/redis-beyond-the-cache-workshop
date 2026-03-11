package com.redis.workshop.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkshopContentLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsManifestAndViewFromWorkshopSourcePath() throws IOException {
        Path moduleRoot = writeWorkshopContent(
            """
                schemaVersion: 1
                viewId: session-home
                route: /welcome
                pageType: stage-flow
                title: Session Management
                slot: instructions
                header:
                  showHubLink: true
                  stageNav:
                    clickable: true
                    steps:
                      - stageId: intro
                        label: Intro
                stages:
                  - stageId: intro
                    title: Introduction
                    sections:
                      - sectionId: overview
                        blocks:
                          - type: markdown
                            body: Welcome to the workshop.
                          - type: stepList
                            listId: intro-steps
                            variant: ordered
                            items:
                              - itemId: open-editor
                                title: Open the editor
                                body: Continue in the editor.
                                actions:
                                  - id: openEditor
                                    label: Open Code Editor
                """
        );

        WorkshopContentLoader loader = loader(moduleRoot);

        WorkshopContentManifestDocument manifest = loader.loadManifest().orElseThrow();
        WorkshopContentViewDocument view = loader.requireView("session-home");

        assertThat(manifest.workshopId()).isEqualTo("1_session_management");
        assertThat(manifest.views()).singleElement().satisfies(entry -> {
            assertThat(entry.viewId()).isEqualTo("session-home");
            assertThat(entry.route()).isEqualTo("/welcome");
            assertThat(entry.pageType()).isEqualTo(WorkshopContentPageType.STAGE_FLOW);
        });

        assertThat(view.title()).isEqualTo("Session Management");
        assertThat(view.pageType()).isEqualTo(WorkshopContentPageType.STAGE_FLOW);
        assertThat(view.stages()).hasSize(1);
        assertThat(view.stages().getFirst().sections()).hasSize(1);
        assertThat(view.stages().getFirst().sections().getFirst().blocks())
            .hasSize(2)
            .first()
            .isInstanceOf(WorkshopMarkdownBlock.class);
    }

    @Test
    void rejectsMalformedViewDocument() throws IOException {
        Path moduleRoot = writeWorkshopContent(
            """
                schemaVersion: 1
                viewId: session-home
                route: /wrong
                pageType: stage-flow
                title: Session Management
                slot: instructions
                stages:
                  - stageId: intro
                    title: Introduction
                    sections:
                      - sectionId: overview
                        blocks:
                          - type: markdown
                            body: Welcome to the workshop.
                """
        );

        WorkshopContentLoader loader = loader(moduleRoot);

        assertThatThrownBy(() -> loader.requireView("session-home"))
            .isInstanceOf(WorkshopContentValidationException.class)
            .hasMessageContaining("must declare route /welcome");
    }

    @Test
    void rejectsOpenFileActionsThatDoNotMatchEditableFiles() throws IOException {
        Path moduleRoot = writeEditorWorkshopContent(
            """
                schemaVersion: 1
                viewId: memory-editor
                route: /editor
                pageType: editor
                title: Agent Memory Workshop
                slot: instructions
                sections:
                  - sectionId: editor-workflow
                    blocks:
                      - type: editorStepList
                        listId: sdk-client
                        title: Step 1
                        items:
                          - itemId: open-missing-file
                            body: Open a file that is not part of the editor tabs.
                            action:
                              id: openFile
                              label: Open File
                              args:
                                file: MissingFile.java
                """
        );

        WorkshopContentLoader loader = loader(moduleRoot, Set.of("AgentMemoryService.java"));

        assertThatThrownBy(() -> loader.requireView("memory-editor"))
            .isInstanceOf(WorkshopContentValidationException.class)
            .hasMessageContaining("openFile.args.file MissingFile.java")
            .hasMessageContaining("editableFiles[].name");
    }

    @Test
    void rejectsRawHtmlInMarkdownEnabledFields() throws IOException {
        Path moduleRoot = writeWorkshopContent(
            """
                schemaVersion: 1
                viewId: session-home
                route: /welcome
                pageType: stage-flow
                title: Session Management
                slot: instructions
                stages:
                  - stageId: intro
                    title: Introduction
                    sections:
                      - sectionId: overview
                        blocks:
                          - type: markdown
                            body: <strong>Unsafe</strong> markup is not allowed.
                """
        );

        WorkshopContentLoader loader = loader(moduleRoot);

        assertThatThrownBy(() -> loader.requireView("session-home"))
            .isInstanceOf(WorkshopContentValidationException.class)
            .hasMessageContaining("markdown.body must not contain raw HTML");
    }

    private Path writeWorkshopContent(String viewDocument) throws IOException {
        Path moduleRoot = Files.createDirectories(tempDir.resolve("session-frontend"));
        Path contentRoot = Files.createDirectories(moduleRoot.resolve("src/main/resources/workshop-content/views"));

        Files.writeString(
            contentRoot.getParent().resolve("manifest.yaml"),
            """
                schemaVersion: 1
                workshopId: 1_session_management
                views:
                  - viewId: session-home
                    route: /welcome
                    pageType: stage-flow
                    file: views/session-home.yaml
                """
        );
        Files.writeString(contentRoot.resolve("session-home.yaml"), viewDocument);
        return moduleRoot;
    }

    private Path writeEditorWorkshopContent(String viewDocument) throws IOException {
        Path moduleRoot = Files.createDirectories(tempDir.resolve("memory-frontend"));
        Path contentRoot = Files.createDirectories(moduleRoot.resolve("src/main/resources/workshop-content/views"));

        Files.writeString(
            contentRoot.getParent().resolve("manifest.yaml"),
            """
                schemaVersion: 1
                workshopId: 4_agent_memory
                views:
                  - viewId: memory-editor
                    route: /editor
                    pageType: editor
                    file: views/memory-editor.yaml
                """
        );
        Files.writeString(contentRoot.resolve("memory-editor.yaml"), viewDocument);
        return moduleRoot;
    }

    private static FrontendRuntimeProperties runtimeProperties(Path moduleRoot) {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSourcePath(moduleRoot.toString());
        return runtimeProperties;
    }

    private static WorkshopContentLoader loader(Path moduleRoot) {
        return loader(moduleRoot, Set.of());
    }

    private static WorkshopContentLoader loader(Path moduleRoot, Set<String> editableFileNames) {
        return new WorkshopContentLoader(
            new MockEnvironment(),
            new DefaultResourceLoader(),
            runtimeProperties(moduleRoot),
            () -> editableFileNames
        );
    }
}

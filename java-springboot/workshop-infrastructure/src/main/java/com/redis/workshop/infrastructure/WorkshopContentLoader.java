package com.redis.workshop.infrastructure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Component
public class WorkshopContentLoader {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    private static final Pattern RAW_HTML_PATTERN = Pattern.compile(
        "(?is)<!--.*?-->|<\\/?[a-z][a-z0-9:_-]*(?:\\s+[^<>]*)?\\s*/?>"
    );

    private static final List<String> DEFAULT_LOCATIONS = List.of(
        "classpath:workshop-content/manifest.yaml",
        "classpath:workshop-content/manifest.yml"
    );

    private final Environment environment;
    private final ResourceLoader resourceLoader;
    private final FrontendRuntimeProperties runtimeProperties;
    private final Supplier<Set<String>> editableFileNamesSupplier;

    @Autowired
    public WorkshopContentLoader(
        Environment environment,
        ResourceLoader resourceLoader,
        FrontendRuntimeProperties runtimeProperties,
        ObjectProvider<WorkshopConfig> workshopConfigProvider
    ) {
        this(environment, resourceLoader, runtimeProperties, () -> {
            WorkshopConfig workshopConfig = workshopConfigProvider.getIfAvailable();
            if (workshopConfig == null) {
                return Set.of();
            }
            return Set.copyOf(workshopConfig.getEditableFiles().keySet());
        });
    }

    WorkshopContentLoader(
        Environment environment,
        ResourceLoader resourceLoader,
        FrontendRuntimeProperties runtimeProperties,
        Supplier<Set<String>> editableFileNamesSupplier
    ) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.runtimeProperties = runtimeProperties;
        this.editableFileNamesSupplier = editableFileNamesSupplier;
    }

    public java.util.Optional<WorkshopContentManifestDocument> loadManifest() {
        return locateManifest().map(this::readManifest);
    }

    public WorkshopContentManifestDocument requireManifest() {
        return readManifest(requireManifestSource());
    }

    public WorkshopContentViewDocument requireView(String viewId) {
        if (!StringUtils.hasText(viewId)) {
            throw new WorkshopContentViewNotFoundException("A workshop content view id is required");
        }

        ManifestBundle bundle = requireManifestBundle();
        WorkshopContentManifestView manifestView = bundle.manifest().findView(viewId.trim())
            .orElseThrow(() -> new WorkshopContentViewNotFoundException(
                "Workshop content view " + viewId + " is not declared in " + bundle.source().location()
            ));

        Resource viewResource = resolveViewResource(bundle.source(), manifestView.file());
        if (!viewResource.exists()) {
            throw new WorkshopContentValidationException(
                "Workshop content view file " + manifestView.file() + " for " + manifestView.viewId()
                    + " does not exist"
            );
        }

        WorkshopContentViewDocument view = readYaml(
            viewResource,
            WorkshopContentViewDocument.class,
            manifestView.file()
        );
        Set<String> editableFileNames = editableFileNamesSupplier.get();
        validateView(
            view,
            manifestView,
            bundle.source().location(),
            editableFileNames == null ? Set.of() : editableFileNames
        );
        return view;
    }

    private ManifestBundle requireManifestBundle() {
        ContentSource manifestSource = requireManifestSource();
        return new ManifestBundle(manifestSource, readManifest(manifestSource));
    }

    private WorkshopContentManifestDocument readManifest(ContentSource manifestSource) {
        WorkshopContentManifestDocument manifest = readYaml(
            manifestSource.resource(),
            WorkshopContentManifestDocument.class,
            manifestSource.location()
        );
        validateManifest(manifest, manifestSource.location());
        return manifest;
    }

    private ContentSource requireManifestSource() {
        return locateManifest()
            .orElseThrow(() -> new WorkshopContentNotFoundException(
                "No workshop content manifest was found in " + String.join(", ", candidateLocations())
            ));
    }

    private java.util.Optional<ContentSource> locateManifest() {
        for (String location : candidateLocations()) {
            Resource resource = resolveResource(location);
            if (resource.exists()) {
                return java.util.Optional.of(new ContentSource(location, resource));
            }
        }
        return java.util.Optional.empty();
    }

    private List<String> candidateLocations() {
        List<String> candidates = new ArrayList<>();
        runtimeProperties.resolveSourcePath().ifPresent(sourcePath -> {
            Path contentRoot = sourcePath.resolve("src/main/resources/workshop-content");
            candidates.add(contentRoot.resolve("manifest.yaml").toString());
            candidates.add(contentRoot.resolve("manifest.yml").toString());
        });
        candidates.addAll(DEFAULT_LOCATIONS);
        return candidates;
    }

    private Resource resolveViewResource(ContentSource manifestSource, String viewFile) {
        if (!StringUtils.hasText(viewFile)) {
            throw new WorkshopContentValidationException(
                "Workshop content manifest at " + manifestSource.location() + " must define a view file"
            );
        }

        Path relativeViewPath = Path.of(viewFile).normalize();
        if (relativeViewPath.isAbsolute() || startsOutsideRoot(relativeViewPath)) {
            throw new WorkshopContentValidationException(
                "Workshop content view file " + viewFile + " in " + manifestSource.location()
                    + " must stay within workshop-content/"
            );
        }

        if (manifestSource.location().startsWith("classpath:")) {
            String manifestPath = manifestSource.location().substring("classpath:".length());
            Path parent = Path.of(manifestPath).getParent();
            String prefix = parent == null ? "" : parent.toString().replace('\\', '/') + "/";
            return resourceLoader.getResource("classpath:" + prefix + relativeViewPath.toString().replace('\\', '/'));
        }

        try {
            Path manifestPath = manifestSource.resource().getFile().toPath();
            Path resolvedPath = manifestPath.getParent().resolve(relativeViewPath).normalize();
            if (!resolvedPath.startsWith(manifestPath.getParent())) {
                throw new WorkshopContentValidationException(
                    "Workshop content view file " + viewFile + " in " + manifestSource.location()
                        + " escapes the workshop-content directory"
                );
            }
            return new FileSystemResource(resolvedPath);
        } catch (IOException e) {
            throw new WorkshopContentValidationException(
                "Failed to resolve workshop content view file " + viewFile + " from " + manifestSource.location(),
                e
            );
        }
    }

    private Resource resolveResource(String location) {
        if (location.startsWith("classpath:") || location.startsWith("file:")) {
            return resourceLoader.getResource(location);
        }
        return new FileSystemResource(Path.of(location).toAbsolutePath().normalize());
    }

    private <T> T readYaml(Resource resource, Class<T> type, String description) {
        try (InputStream inputStream = resource.getInputStream()) {
            return YAML_MAPPER.readValue(inputStream, type);
        } catch (IOException e) {
            throw new WorkshopContentValidationException(
                "Failed to read workshop content from " + description,
                e
            );
        }
    }

    private static void validateManifest(WorkshopContentManifestDocument manifest, String location) {
        if (manifest == null) {
            throw new WorkshopContentValidationException("Workshop content manifest at " + location + " is empty");
        }
        if (manifest.schemaVersion() != 1) {
            throw new WorkshopContentValidationException(
                "Workshop content manifest at " + location + " must declare schemaVersion: 1"
            );
        }
        if (!StringUtils.hasText(manifest.workshopId())) {
            throw new WorkshopContentValidationException(
                "Workshop content manifest at " + location + " must define workshopId"
            );
        }
        if (manifest.views().isEmpty()) {
            throw new WorkshopContentValidationException(
                "Workshop content manifest at " + location + " must define at least one view"
            );
        }

        Set<String> viewIds = new HashSet<>();
        Set<String> routes = new HashSet<>();
        for (WorkshopContentManifestView manifestView : manifest.views()) {
            if (manifestView == null) {
                throw new WorkshopContentValidationException(
                    "Workshop content manifest at " + location + " contains a null view entry"
                );
            }
            if (!StringUtils.hasText(manifestView.viewId())) {
                throw new WorkshopContentValidationException(
                    "Workshop content manifest at " + location + " contains a view without viewId"
                );
            }
            if (!viewIds.add(manifestView.viewId())) {
                throw new WorkshopContentValidationException(
                    "Workshop content manifest at " + location + " contains duplicate viewId " + manifestView.viewId()
                );
            }
            if (!StringUtils.hasText(manifestView.route()) || !manifestView.route().startsWith("/")) {
                throw new WorkshopContentValidationException(
                    "Workshop content manifest at " + location + " must define a route starting with / for "
                        + manifestView.viewId()
                );
            }
            if (!routes.add(manifestView.route())) {
                throw new WorkshopContentValidationException(
                    "Workshop content manifest at " + location + " contains duplicate route " + manifestView.route()
                );
            }
            if (manifestView.pageType() == null) {
                throw new WorkshopContentValidationException(
                    "Workshop content manifest at " + location + " must define pageType for " + manifestView.viewId()
                );
            }
            if (!StringUtils.hasText(manifestView.file())) {
                throw new WorkshopContentValidationException(
                    "Workshop content manifest at " + location + " must define file for " + manifestView.viewId()
                );
            }
            Path filePath = Path.of(manifestView.file()).normalize();
            if (filePath.isAbsolute() || startsOutsideRoot(filePath)) {
                throw new WorkshopContentValidationException(
                    "Workshop content manifest at " + location + " contains an invalid file path for "
                        + manifestView.viewId()
                );
            }
        }
    }

    private static void validateView(
        WorkshopContentViewDocument view,
        WorkshopContentManifestView manifestView,
        String manifestLocation,
        Set<String> editableFileNames
    ) {
        if (view == null) {
            throw new WorkshopContentValidationException(
                "Workshop content view file " + manifestView.file() + " declared in " + manifestLocation + " is empty"
            );
        }
        if (view.schemaVersion() != 1) {
            throw new WorkshopContentValidationException(
                "Workshop content view " + manifestView.viewId() + " must declare schemaVersion: 1"
            );
        }
        if (!manifestView.viewId().equals(view.viewId())) {
            throw new WorkshopContentValidationException(
                "Workshop content view " + manifestView.file() + " must declare viewId " + manifestView.viewId()
            );
        }
        if (!manifestView.route().equals(view.route())) {
            throw new WorkshopContentValidationException(
                "Workshop content view " + manifestView.file() + " must declare route " + manifestView.route()
            );
        }
        if (view.pageType() != manifestView.pageType()) {
            throw new WorkshopContentValidationException(
                "Workshop content view " + manifestView.file() + " must declare pageType " + manifestView.pageType().value()
            );
        }
        if (!StringUtils.hasText(view.title())) {
            throw new WorkshopContentValidationException(
                "Workshop content view " + manifestView.file() + " must define title"
            );
        }
        rejectRawHtml(
            view.title(),
            "Workshop content view " + manifestView.file() + " title must not contain raw HTML"
        );
        if (view.slot() == null) {
            throw new WorkshopContentValidationException(
                "Workshop content view " + manifestView.file() + " must define slot"
            );
        }
        rejectRawHtml(
            view.summary(),
            "Workshop content view " + manifestView.file() + " summary must not contain raw HTML"
        );

        switch (view.pageType()) {
            case NARRATIVE, EDITOR -> {
                if (view.sections().isEmpty()) {
                    throw new WorkshopContentValidationException(
                        "Workshop content view " + manifestView.file() + " must define sections"
                    );
                }
                if (!view.stages().isEmpty()) {
                    throw new WorkshopContentValidationException(
                        "Workshop content view " + manifestView.file() + " must not define stages for pageType "
                            + view.pageType().value()
                    );
                }
            }
            case STAGE_FLOW -> {
                if (view.stages().isEmpty()) {
                    throw new WorkshopContentValidationException(
                        "Workshop content view " + manifestView.file() + " must define stages"
                    );
                }
                if (!view.sections().isEmpty()) {
                    throw new WorkshopContentValidationException(
                        "Workshop content view " + manifestView.file() + " must not define sections for pageType stage-flow"
                    );
                }
            }
        }

        validateStageNav(view, manifestView.file());

        if (!view.sections().isEmpty()) {
            validateSections(view.sections(), manifestView.file(), "view " + view.viewId(), editableFileNames);
        }
        if (!view.stages().isEmpty()) {
            Set<String> stageIds = new HashSet<>();
            for (WorkshopContentStage stage : view.stages()) {
                if (stage == null) {
                    throw new WorkshopContentValidationException(
                        "Workshop content view " + manifestView.file() + " contains a null stage entry"
                    );
                }
                if (!StringUtils.hasText(stage.stageId())) {
                    throw new WorkshopContentValidationException(
                        "Workshop content view " + manifestView.file() + " contains a stage without stageId"
                    );
                }
                if (!stageIds.add(stage.stageId())) {
                    throw new WorkshopContentValidationException(
                        "Workshop content view " + manifestView.file() + " contains duplicate stageId " + stage.stageId()
                    );
                }
                if (!StringUtils.hasText(stage.title())) {
                    throw new WorkshopContentValidationException(
                        "Workshop content stage " + stage.stageId() + " in " + manifestView.file() + " must define title"
                    );
                }
                rejectRawHtml(
                    stage.title(),
                    "Workshop content stage " + stage.stageId() + " in " + manifestView.file()
                        + " title must not contain raw HTML"
                );
                if (stage.sections().isEmpty()) {
                    throw new WorkshopContentValidationException(
                        "Workshop content stage " + stage.stageId() + " in " + manifestView.file() + " must define sections"
                    );
                }
                validateSections(stage.sections(), manifestView.file(), "stage " + stage.stageId(), editableFileNames);
            }
        }
    }

    private static void validateStageNav(WorkshopContentViewDocument view, String viewFile) {
        if (view.header() == null || view.header().stageNav() == null || view.header().stageNav().steps().isEmpty()) {
            return;
        }

        Set<String> stageIds = new HashSet<>();
        for (WorkshopContentStage stage : view.stages()) {
            stageIds.add(stage.stageId());
        }

        for (WorkshopContentStageNavStep step : view.header().stageNav().steps()) {
            if (step == null || !StringUtils.hasText(step.stageId())) {
                throw new WorkshopContentValidationException(
                    "Workshop content stageNav in " + viewFile + " contains a step without stageId"
                );
            }
            rejectRawHtml(
                step.label(),
                "Workshop content stageNav step " + step.stageId() + " in " + viewFile
                    + " label must not contain raw HTML"
            );
            if (!stageIds.contains(step.stageId())) {
                throw new WorkshopContentValidationException(
                    "Workshop content stageNav step " + step.stageId() + " in " + viewFile
                        + " must match a stageId in stages[]"
                );
            }
        }
    }

    private static void validateSections(
        List<WorkshopContentSection> sections,
        String viewFile,
        String locationLabel,
        Set<String> editableFileNames
    ) {
        Set<String> sectionIds = new HashSet<>();
        for (WorkshopContentSection section : sections) {
            if (section == null) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile + " contains a null section"
                );
            }
            if (!StringUtils.hasText(section.sectionId())) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile + " contains a section without sectionId"
                );
            }
            if (!sectionIds.add(section.sectionId())) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile + " contains duplicate sectionId "
                        + section.sectionId()
                );
            }
            rejectRawHtml(
                section.title(),
                "Workshop content section " + section.sectionId() + " in " + viewFile
                    + " title must not contain raw HTML"
            );
            rejectRawHtml(
                section.body(),
                "Workshop content section " + section.sectionId() + " in " + viewFile
                    + " body must not contain raw HTML"
            );
            validateBlocks(section.blocks(), viewFile, "section " + section.sectionId(), false, editableFileNames);
        }
    }

    private static void validateBlocks(
        List<WorkshopContentBlock> blocks,
        String viewFile,
        String locationLabel,
        boolean supportingContext,
        Set<String> editableFileNames
    ) {
        for (WorkshopContentBlock block : blocks) {
            if (block == null) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile + " contains a null block"
                );
            }
            if (supportingContext && block instanceof WorkshopMarkdownBlock) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile
                        + " may only use codeSnippet, callout, or widget inside supportingBlocks"
                );
            }
            if (supportingContext && block instanceof WorkshopStepListBlock) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile
                        + " may only use codeSnippet, callout, or widget inside supportingBlocks"
                );
            }
            if (supportingContext && block instanceof WorkshopEditorStepListBlock) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile
                        + " may only use codeSnippet, callout, or widget inside supportingBlocks"
                );
            }

            switch (block) {
                case WorkshopMarkdownBlock markdownBlock -> {
                    requireText(
                        markdownBlock.body(),
                        "Workshop content " + locationLabel + " in " + viewFile + " must define markdown.body"
                    );
                    rejectRawHtml(
                        markdownBlock.body(),
                        "Workshop content " + locationLabel + " in " + viewFile + " markdown.body must not contain raw HTML"
                    );
                }
                case WorkshopCalloutBlock calloutBlock -> {
                    if (calloutBlock.tone() == null) {
                        throw new WorkshopContentValidationException(
                            "Workshop content " + locationLabel + " in " + viewFile + " must define callout.tone"
                        );
                    }
                    requireText(
                        calloutBlock.body(),
                        "Workshop content " + locationLabel + " in " + viewFile + " must define callout.body"
                    );
                    rejectRawHtml(
                        calloutBlock.title(),
                        "Workshop content " + locationLabel + " in " + viewFile + " callout.title must not contain raw HTML"
                    );
                    rejectRawHtml(
                        calloutBlock.body(),
                        "Workshop content " + locationLabel + " in " + viewFile + " callout.body must not contain raw HTML"
                    );
                    validateActions(calloutBlock.actions(), viewFile, locationLabel + " callout", editableFileNames);
                }
                case WorkshopStepListBlock stepListBlock -> validateStepList(
                    stepListBlock,
                    viewFile,
                    locationLabel,
                    editableFileNames
                );
                case WorkshopEditorStepListBlock editorStepListBlock -> validateEditorStepList(
                    editorStepListBlock,
                    viewFile,
                    locationLabel,
                    editableFileNames
                );
                case WorkshopCodeSnippetBlock codeSnippetBlock -> {
                    requireText(
                        codeSnippetBlock.language(),
                        "Workshop content " + locationLabel + " in " + viewFile + " must define codeSnippet.language"
                    );
                    requireText(
                        codeSnippetBlock.code(),
                        "Workshop content " + locationLabel + " in " + viewFile + " must define codeSnippet.code"
                    );
                    rejectRawHtml(
                        codeSnippetBlock.title(),
                        "Workshop content " + locationLabel + " in " + viewFile + " codeSnippet.title must not contain raw HTML"
                    );
                    rejectRawHtml(
                        codeSnippetBlock.caption(),
                        "Workshop content " + locationLabel + " in " + viewFile + " codeSnippet.caption must not contain raw HTML"
                    );
                }
                case WorkshopWidgetBlock widgetBlock -> requireText(
                    widgetBlock.widgetId(),
                    "Workshop content " + locationLabel + " in " + viewFile + " must define widget.widgetId"
                );
                default -> throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile + " contains an unsupported block type "
                        + block.getClass().getSimpleName()
                );
            }
        }
    }

    private static void validateStepList(
        WorkshopStepListBlock block,
        String viewFile,
        String locationLabel,
        Set<String> editableFileNames
    ) {
        requireText(
            block.listId(),
            "Workshop content " + locationLabel + " in " + viewFile + " must define stepList.listId"
        );
        if (block.variant() == null) {
            throw new WorkshopContentValidationException(
                "Workshop content " + locationLabel + " in " + viewFile + " must define stepList.variant"
            );
        }
        if (block.items().isEmpty()) {
            throw new WorkshopContentValidationException(
                "Workshop content " + locationLabel + " in " + viewFile + " must define stepList.items"
            );
        }

        Set<String> itemIds = new HashSet<>();
        for (WorkshopStepListItem item : block.items()) {
            if (item == null) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile + " contains a null stepList item"
                );
            }
            requireText(
                item.itemId(),
                "Workshop content " + locationLabel + " in " + viewFile + " contains a stepList item without itemId"
            );
            if (!itemIds.add(item.itemId())) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile + " contains duplicate stepList itemId "
                        + item.itemId()
                );
            }
            requireText(
                item.title(),
                "Workshop content stepList item " + item.itemId() + " in " + viewFile + " must define title"
            );
            rejectRawHtml(
                item.title(),
                "Workshop content stepList item " + item.itemId() + " in " + viewFile + " title must not contain raw HTML"
            );
            requireText(
                item.body(),
                "Workshop content stepList item " + item.itemId() + " in " + viewFile + " must define body"
            );
            rejectRawHtml(
                item.body(),
                "Workshop content stepList item " + item.itemId() + " in " + viewFile + " body must not contain raw HTML"
            );
            rejectRawHtml(
                item.hint(),
                "Workshop content stepList item " + item.itemId() + " in " + viewFile + " hint must not contain raw HTML"
            );
            validateActions(item.actions(), viewFile, "stepList item " + item.itemId(), editableFileNames);
            validateBlocks(item.supportingBlocks(), viewFile, "stepList item " + item.itemId(), true, editableFileNames);
        }
    }

    private static void validateEditorStepList(
        WorkshopEditorStepListBlock block,
        String viewFile,
        String locationLabel,
        Set<String> editableFileNames
    ) {
        requireText(
            block.listId(),
            "Workshop content " + locationLabel + " in " + viewFile + " must define editorStepList.listId"
        );
        requireText(
            block.title(),
            "Workshop content " + locationLabel + " in " + viewFile + " must define editorStepList.title"
        );
        rejectRawHtml(
            block.title(),
            "Workshop content " + locationLabel + " in " + viewFile + " editorStepList.title must not contain raw HTML"
        );
        rejectRawHtml(
            block.description(),
            "Workshop content " + locationLabel + " in " + viewFile + " editorStepList.description must not contain raw HTML"
        );
        if (block.items().isEmpty()) {
            throw new WorkshopContentValidationException(
                "Workshop content " + locationLabel + " in " + viewFile + " must define editorStepList.items"
            );
        }

        Set<String> itemIds = new HashSet<>();
        for (WorkshopEditorStepListItem item : block.items()) {
            if (item == null) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile + " contains a null editorStepList item"
                );
            }
            requireText(
                item.itemId(),
                "Workshop content " + locationLabel + " in " + viewFile + " contains an editorStepList item without itemId"
            );
            if (!itemIds.add(item.itemId())) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile
                        + " contains duplicate editorStepList itemId " + item.itemId()
                );
            }
            requireText(
                item.body(),
                "Workshop content editorStepList item " + item.itemId() + " in " + viewFile + " must define body"
            );
            rejectRawHtml(
                item.body(),
                "Workshop content editorStepList item " + item.itemId() + " in " + viewFile + " body must not contain raw HTML"
            );
            rejectRawHtml(
                item.hint(),
                "Workshop content editorStepList item " + item.itemId() + " in " + viewFile + " hint must not contain raw HTML"
            );
            if (item.action() != null) {
                validateAction(item.action(), viewFile, "editorStepList item " + item.itemId(), editableFileNames);
            }
        }
    }

    private static void validateActions(
        List<WorkshopContentAction> actions,
        String viewFile,
        String locationLabel,
        Set<String> editableFileNames
    ) {
        for (WorkshopContentAction action : actions) {
            validateAction(action, viewFile, locationLabel, editableFileNames);
        }
    }

    private static void validateAction(
        WorkshopContentAction action,
        String viewFile,
        String locationLabel,
        Set<String> editableFileNames
    ) {
        if (action == null) {
            throw new WorkshopContentValidationException(
                "Workshop content " + locationLabel + " in " + viewFile + " contains a null action"
            );
        }
        if (action.id() == null) {
            throw new WorkshopContentValidationException(
                "Workshop content " + locationLabel + " in " + viewFile + " must define action.id"
            );
        }
        requireText(
            action.label(),
            "Workshop content " + locationLabel + " in " + viewFile + " must define action.label"
        );
        rejectRawHtml(
            action.label(),
            "Workshop content " + locationLabel + " in " + viewFile + " action.label must not contain raw HTML"
        );

        Map<String, String> args = action.args();
        for (String requiredArg : action.id().requiredArgs()) {
            if (!StringUtils.hasText(args.get(requiredArg))) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile + " action " + action.id().value()
                        + " must define args." + requiredArg
                );
            }
        }

        if (action.id() == WorkshopContentActionId.OPEN_FILE) {
            if (editableFileNames.isEmpty()) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile
                        + " uses openFile but no workshop editableFiles metadata is available for validation"
                );
            }

            String targetFile = args.get("file");
            if (!editableFileNames.contains(targetFile)) {
                throw new WorkshopContentValidationException(
                    "Workshop content " + locationLabel + " in " + viewFile + " openFile.args.file " + targetFile
                        + " must match a workshop-manifest editableFiles[].name entry"
                );
            }
        }
    }

    private static void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new WorkshopContentValidationException(message);
        }
    }

    private static void rejectRawHtml(String value, String message) {
        if (StringUtils.hasText(value) && RAW_HTML_PATTERN.matcher(value).find()) {
            throw new WorkshopContentValidationException(message);
        }
    }

    private static boolean startsOutsideRoot(Path path) {
        return path.startsWith("..");
    }

    record ContentSource(String location, Resource resource) {
    }

    record ManifestBundle(ContentSource source, WorkshopContentManifestDocument manifest) {
    }
}

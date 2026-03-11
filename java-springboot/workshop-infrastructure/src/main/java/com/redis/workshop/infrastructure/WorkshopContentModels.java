package com.redis.workshop.infrastructure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

record WorkshopContentManifestDocument(
    int schemaVersion,
    String workshopId,
    List<WorkshopContentManifestView> views
) {

    WorkshopContentManifestDocument {
        views = views == null ? List.of() : List.copyOf(views);
    }

    Optional<WorkshopContentManifestView> findView(String viewId) {
        return views.stream()
            .filter(view -> view != null && viewId.equals(view.viewId()))
            .findFirst();
    }
}

record WorkshopContentManifestView(
    String viewId,
    String route,
    WorkshopContentPageType pageType,
    String file
) {
}

record WorkshopContentViewDocument(
    int schemaVersion,
    String viewId,
    String route,
    WorkshopContentPageType pageType,
    String title,
    WorkshopContentSlot slot,
    String summary,
    List<String> dataRequirements,
    WorkshopContentHeader header,
    List<WorkshopContentSection> sections,
    List<WorkshopContentStage> stages
) {

    WorkshopContentViewDocument {
        dataRequirements = dataRequirements == null ? List.of() : List.copyOf(dataRequirements);
        sections = sections == null ? List.of() : List.copyOf(sections);
        stages = stages == null ? List.of() : List.copyOf(stages);
    }
}

record WorkshopContentHeader(
    Boolean showHubLink,
    WorkshopContentStageNav stageNav
) {
}

record WorkshopContentStageNav(
    Boolean clickable,
    List<WorkshopContentStageNavStep> steps
) {

    WorkshopContentStageNav {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}

record WorkshopContentStageNavStep(
    String stageId,
    String label
) {
}

record WorkshopContentStage(
    String stageId,
    String title,
    List<WorkshopContentSection> sections
) {

    WorkshopContentStage {
        sections = sections == null ? List.of() : List.copyOf(sections);
    }
}

record WorkshopContentSection(
    String sectionId,
    String title,
    String body,
    List<WorkshopContentBlock> blocks
) {

    WorkshopContentSection {
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = WorkshopMarkdownBlock.class, name = "markdown"),
    @JsonSubTypes.Type(value = WorkshopCalloutBlock.class, name = "callout"),
    @JsonSubTypes.Type(value = WorkshopStepListBlock.class, name = "stepList"),
    @JsonSubTypes.Type(value = WorkshopEditorStepListBlock.class, name = "editorStepList"),
    @JsonSubTypes.Type(value = WorkshopCodeSnippetBlock.class, name = "codeSnippet"),
    @JsonSubTypes.Type(value = WorkshopWidgetBlock.class, name = "widget")
})
interface WorkshopContentBlock {
}

@JsonTypeName("markdown")
record WorkshopMarkdownBlock(
    String body
) implements WorkshopContentBlock {
}

@JsonTypeName("callout")
record WorkshopCalloutBlock(
    WorkshopContentTone tone,
    String title,
    String body,
    List<WorkshopContentAction> actions
) implements WorkshopContentBlock {

    WorkshopCalloutBlock {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}

@JsonTypeName("stepList")
record WorkshopStepListBlock(
    String listId,
    WorkshopContentStepListVariant variant,
    List<WorkshopStepListItem> items
) implements WorkshopContentBlock {

    WorkshopStepListBlock {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

record WorkshopStepListItem(
    String itemId,
    String title,
    String body,
    String hint,
    List<WorkshopContentAction> actions,
    List<WorkshopContentBlock> supportingBlocks
) {

    WorkshopStepListItem {
        actions = actions == null ? List.of() : List.copyOf(actions);
        supportingBlocks = supportingBlocks == null ? List.of() : List.copyOf(supportingBlocks);
    }
}

@JsonTypeName("editorStepList")
record WorkshopEditorStepListBlock(
    String listId,
    String title,
    String description,
    Integer startAt,
    String progressGroup,
    List<WorkshopEditorStepListItem> items
) implements WorkshopContentBlock {

    WorkshopEditorStepListBlock {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

record WorkshopEditorStepListItem(
    String itemId,
    String body,
    String hint,
    WorkshopContentAction action
) {
}

@JsonTypeName("codeSnippet")
record WorkshopCodeSnippetBlock(
    String language,
    String title,
    String caption,
    String code
) implements WorkshopContentBlock {
}

@JsonTypeName("widget")
record WorkshopWidgetBlock(
    String widgetId
) implements WorkshopContentBlock {
}

record WorkshopContentAction(
    WorkshopContentActionId id,
    String label,
    Map<String, String> args
) {

    WorkshopContentAction {
        args = args == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(args));
    }
}

enum WorkshopContentPageType {
    NARRATIVE("narrative"),
    STAGE_FLOW("stage-flow"),
    EDITOR("editor");

    private final String value;

    WorkshopContentPageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    static WorkshopContentPageType fromValue(String value) {
        for (WorkshopContentPageType candidate : values()) {
            if (candidate.value.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported pageType: " + value);
    }
}

enum WorkshopContentSlot {
    INSTRUCTIONS("instructions");

    private final String value;

    WorkshopContentSlot(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    static WorkshopContentSlot fromValue(String value) {
        for (WorkshopContentSlot candidate : values()) {
            if (candidate.value.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported slot: " + value);
    }
}

enum WorkshopContentTone {
    INFO("info"),
    SUCCESS("success"),
    WARNING("warning"),
    ERROR("error");

    private final String value;

    WorkshopContentTone(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    static WorkshopContentTone fromValue(String value) {
        for (WorkshopContentTone candidate : values()) {
            if (candidate.value.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported callout tone: " + value);
    }
}

enum WorkshopContentStepListVariant {
    ORDERED("ordered"),
    CHECKLIST("checklist"),
    TESTS("tests");

    private final String value;

    WorkshopContentStepListVariant(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    static WorkshopContentStepListVariant fromValue(String value) {
        for (WorkshopContentStepListVariant candidate : values()) {
            if (candidate.value.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported stepList variant: " + value);
    }
}

enum WorkshopContentActionId {
    OPEN_HUB("openHub"),
    OPEN_EDITOR("openEditor"),
    OPEN_REDIS_INSIGHT("openRedisInsight"),
    OPEN_ROUTE("openRoute", "route"),
    OPEN_FILE("openFile", "file"),
    OPEN_REFERENCE("openReference", "referenceId"),
    SAVE_FILE("saveFile"),
    APPLY_EDITOR_STEP("applyEditorStep", "stepId"),
    MARK_ITEM_COMPLETE("markItemComplete", "groupId", "itemId"),
    SET_STAGE("setStage", "stageId"),
    RUN_CHECK("runCheck", "checkId"),
    RESTART_LAB("restartLab"),
    RESET_PROGRESS("resetProgress");

    private final String value;
    private final Set<String> requiredArgs;

    WorkshopContentActionId(String value, String... requiredArgs) {
        this.value = value;
        this.requiredArgs = Set.of(requiredArgs);
    }

    @JsonValue
    public String value() {
        return value;
    }

    public Set<String> requiredArgs() {
        return requiredArgs;
    }

    @JsonCreator
    static WorkshopContentActionId fromValue(String value) {
        for (WorkshopContentActionId candidate : values()) {
            if (candidate.value.equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported action id: " + value);
    }
}

record WorkshopContentErrorResponse(
    String errorCode,
    String message
) {
}

abstract class WorkshopContentException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    WorkshopContentException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    HttpStatus status() {
        return status;
    }

    String errorCode() {
        return errorCode;
    }
}

final class WorkshopContentNotFoundException extends WorkshopContentException {

    WorkshopContentNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "WORKSHOP_CONTENT_NOT_FOUND", message);
    }
}

final class WorkshopContentViewNotFoundException extends WorkshopContentException {

    WorkshopContentViewNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "WORKSHOP_CONTENT_VIEW_NOT_FOUND", message);
    }
}

final class WorkshopContentValidationException extends WorkshopContentException {

    WorkshopContentValidationException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "WORKSHOP_CONTENT_INVALID", message);
    }

    WorkshopContentValidationException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "WORKSHOP_CONTENT_INVALID", message);
        initCause(cause);
    }
}

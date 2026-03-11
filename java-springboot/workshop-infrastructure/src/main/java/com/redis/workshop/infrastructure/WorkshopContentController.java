package com.redis.workshop.infrastructure;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content")
public class WorkshopContentController {

    private final WorkshopContentLoader workshopContentLoader;

    public WorkshopContentController(WorkshopContentLoader workshopContentLoader) {
        this.workshopContentLoader = workshopContentLoader;
    }

    @GetMapping("/manifest")
    public WorkshopContentManifestDocument manifest() {
        return workshopContentLoader.requireManifest();
    }

    @GetMapping("/views/{viewId}")
    public WorkshopContentViewDocument view(@PathVariable String viewId) {
        return workshopContentLoader.requireView(viewId);
    }

    @ExceptionHandler(WorkshopContentException.class)
    ResponseEntity<WorkshopContentErrorResponse> handleContentException(WorkshopContentException exception) {
        return ResponseEntity.status(exception.status())
            .body(new WorkshopContentErrorResponse(exception.errorCode(), exception.getMessage()));
    }
}

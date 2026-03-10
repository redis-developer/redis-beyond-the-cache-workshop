package com.redis.workshop.session.frontend.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import com.redis.workshop.infrastructure.WorkshopManifestLoader;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SessionManagementWorkshopConfig implements WorkshopConfig {

    private final WorkshopConfig delegate;

    public SessionManagementWorkshopConfig(Environment environment, ResourceLoader resourceLoader) {
        this.delegate = new WorkshopManifestLoader(environment, resourceLoader)
            .loadWorkshopConfig()
            .orElseThrow(() -> new IllegalStateException("Expected workshop-manifest.yaml to define workshop metadata"));
    }

    @Override
    public Map<String, String> getEditableFiles() {
        return delegate.getEditableFiles();
    }

    @Override
    public String getOriginalContent(String fileName) {
        return delegate.getOriginalContent(fileName);
    }

    @Override
    public String getBasePath() {
        return delegate.getBasePath();
    }

    @Override
    public String getModuleName() {
        return delegate.getModuleName();
    }

    @Override
    public String getLanguage(String fileName) {
        return delegate.getLanguage(fileName);
    }

    @Override
    public String getWorkshopTitle() {
        return delegate.getWorkshopTitle();
    }

    @Override
    public String getWorkshopDescription() {
        return delegate.getWorkshopDescription();
    }
}

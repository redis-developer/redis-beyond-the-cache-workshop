package com.redis.workshop.search.frontend.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import com.redis.workshop.infrastructure.WorkshopManifestLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

@Configuration
public class FullTextSearchManifestConfiguration {

    @Bean
    @ConditionalOnMissingBean(WorkshopConfig.class)
    WorkshopConfig workshopConfig(WorkshopManifestLoader workshopManifestLoader) {
        return workshopManifestLoader.loadWorkshopConfig()
            .orElseThrow(() -> new IllegalStateException("Expected workshop-manifest.yaml to be available"));
    }
}

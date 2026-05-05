package com.redis.workshop.springai.multiagents.frontend.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import com.redis.workshop.infrastructure.WorkshopManifestLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BuildingMultiAgentsWorkshopManifestConfiguration {

    @Bean
    @ConditionalOnMissingBean(WorkshopConfig.class)
    WorkshopConfig workshopConfig(WorkshopManifestLoader workshopManifestLoader) {
        return workshopManifestLoader.loadWorkshopConfig()
            .orElseThrow(() -> new IllegalStateException("Expected workshop-manifest.yaml to be available"));
    }
}

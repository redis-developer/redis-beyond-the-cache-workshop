package com.redis.workshop.memory.frontend.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import com.redis.workshop.infrastructure.WorkshopManifestLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

@Configuration(proxyBeanMethods = false)
public class AgentMemoryWorkshopConfig {

    @Bean
    WorkshopConfig agentMemoryWorkshopManifest(Environment environment, ResourceLoader resourceLoader) {
        return new WorkshopManifestLoader(environment, resourceLoader)
            .loadWorkshopConfig()
            .orElseThrow(() -> new IllegalStateException("Expected workshop-manifest.yaml for the agent memory frontend"));
    }
}

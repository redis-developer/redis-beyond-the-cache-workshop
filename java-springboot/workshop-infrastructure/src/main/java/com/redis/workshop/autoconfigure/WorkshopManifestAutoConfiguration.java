package com.redis.workshop.autoconfigure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import com.redis.workshop.infrastructure.WorkshopManifestLoader;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;

@AutoConfiguration
public class WorkshopManifestAutoConfiguration {

    @Bean
    WorkshopManifestLoader workshopManifestLoader(
        org.springframework.core.env.Environment environment,
        ResourceLoader resourceLoader
    ) {
        return new WorkshopManifestLoader(environment, resourceLoader);
    }

    @Bean
    @ConditionalOnMissingBean(WorkshopConfig.class)
    @Conditional(WorkshopManifestPresentCondition.class)
    WorkshopConfig manifestWorkshopConfig(WorkshopManifestLoader workshopManifestLoader) {
        return workshopManifestLoader.loadWorkshopConfig()
            .orElseThrow(() -> new IllegalStateException("Expected a workshop manifest to be available"));
    }

    static final class WorkshopManifestPresentCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ResourceLoader resourceLoader = context.getResourceLoader();
            if (resourceLoader == null) {
                resourceLoader = new DefaultResourceLoader();
            }
            return WorkshopManifestLoader.hasManifest(context.getEnvironment(), resourceLoader);
        }
    }
}

package com.redis.workshop.platform.controlplane.observability;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public class ObservabilityConfiguration implements WebMvcConfigurer {

    private final SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder;

    public ObservabilityConfiguration(SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder) {
        this.sessionLifecycleMetricsRecorder = sessionLifecycleMetricsRecorder;
    }

    @Bean
    SessionLifecycleMetricsInterceptor sessionLifecycleMetricsInterceptor() {
        return new SessionLifecycleMetricsInterceptor(sessionLifecycleMetricsRecorder);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionLifecycleMetricsInterceptor())
            .addPathPatterns("/api/sessions", "/api/sessions/**");
    }
}

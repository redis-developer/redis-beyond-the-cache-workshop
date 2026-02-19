package com.redis.workshop.hub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.frontend.url:http://localhost:3000}")
    private String appFrontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost",
                        "http://localhost:80",
                        "http://localhost:3000",
                        "http://localhost:3001",
                        "http://localhost:9000",
                        appFrontendUrl  // Dynamic from environment variable
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve Vue.js static assets
        registry.addResourceHandler("/css/**", "/js/**", "/img/**", "/fonts/**")
                .addResourceLocations("classpath:/static/vue/css/", "classpath:/static/vue/js/",
                                     "classpath:/static/vue/img/", "classpath:/static/vue/fonts/")
                .setCachePeriod(3600);

        // Fallback to index.html for Vue Router (SPA)
        // But exclude /manager/api/** which should go to controllers
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/vue/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // Don't intercept API calls - let them go to controllers
                        if (resourcePath.startsWith("manager/api/")) {
                            return null;
                        }

                        Resource requestedResource = location.createRelative(resourcePath);

                        // If resource exists, return it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // Otherwise, return index.html for Vue Router
                        return new ClassPathResource("/static/vue/index.html");
                    }
                });
    }
}


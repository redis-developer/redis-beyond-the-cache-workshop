package com.redis.workshop.infrastructure;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class CodeEditorWebSocketProxyConfig {

    @Bean
    CodeEditorWebSocketProxyHandler codeEditorWebSocketProxyHandler(SessionRunnerProperties runnerProperties) {
        return new CodeEditorWebSocketProxyHandler(runnerProperties);
    }

    @Bean
    FilterRegistrationBean<Filter> codeEditorWebSocketProxyFilter(CodeEditorWebSocketProxyHandler handler) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter((request, response, chain) -> {
            if (request instanceof HttpServletRequest httpRequest
                && response instanceof HttpServletResponse httpResponse
                && handler.canHandle(httpRequest)) {
                handler.handle(httpRequest, httpResponse);
                return;
            }
            chain.doFilter(request, response);
        });
        registration.addUrlPatterns("/code", "/code/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}

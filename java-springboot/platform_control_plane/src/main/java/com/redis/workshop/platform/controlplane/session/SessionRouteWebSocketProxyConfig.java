package com.redis.workshop.platform.controlplane.session;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class SessionRouteWebSocketProxyConfig {

    @Bean
    FilterRegistrationBean<Filter> sessionRouteWebSocketProxyFilter(
        SessionRouteWebSocketProxyHandler handler
    ) {
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
        registration.addUrlPatterns("/session/*");
        registration.setAsyncSupported(true);
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 20);
        return registration;
    }
}

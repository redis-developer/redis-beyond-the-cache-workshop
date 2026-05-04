package com.redis.workshop.platform.controlplane.security;

import com.redis.workshop.platform.controlplane.portal.PortalSessionProperties;
import com.redis.workshop.platform.controlplane.portal.PortalSessionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableConfigurationProperties(PortalSessionProperties.class)
public class SecurityConfiguration {

    @Bean
    HeaderAuthenticatedActorFilter headerAuthenticatedActorFilter() {
        return new HeaderAuthenticatedActorFilter();
    }

    @Bean
    PortalSessionAuthenticationFilter portalSessionAuthenticationFilter(
        ObjectProvider<PortalSessionService> portalSessionService,
        PortalSessionProperties portalSessionProperties
    ) {
        return new PortalSessionAuthenticationFilter(portalSessionService::getIfAvailable, portalSessionProperties);
    }

    @Bean
    CurrentActorProvider currentActorProvider() {
        return new SecurityContextCurrentActorProvider();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        PortalSessionAuthenticationFilter portalSessionAuthenticationFilter,
        HeaderAuthenticatedActorFilter headerAuthenticatedActorFilter
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET,
                    "/",
                    "/console",
                    "/index.html",
                    "/favicon.ico",
                    "/css/**",
                    "/js/**",
                    "/fonts/**",
                    "/img/**",
                    "/assets/**",
                    "/manifest.json",
                    "/robots.txt"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/portal/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/portal/me").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/portal/logout").permitAll()
                .requestMatchers("/session/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/sessions").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/sessions/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/sessions").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/sessions/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/sessions/**").authenticated()
                .anyRequest().denyAll()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        http.addFilterBefore(portalSessionAuthenticationFilter, AnonymousAuthenticationFilter.class);
        http.addFilterBefore(headerAuthenticatedActorFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }
}

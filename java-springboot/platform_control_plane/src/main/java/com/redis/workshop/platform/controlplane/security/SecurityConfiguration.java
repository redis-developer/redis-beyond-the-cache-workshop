package com.redis.workshop.platform.controlplane.security;

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
public class SecurityConfiguration {

    @Bean
    HeaderAuthenticatedActorFilter headerAuthenticatedActorFilter() {
        return new HeaderAuthenticatedActorFilter();
    }

    @Bean
    CurrentActorProvider currentActorProvider() {
        return new SecurityContextCurrentActorProvider();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
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
            )
            .addFilterBefore(headerAuthenticatedActorFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }
}

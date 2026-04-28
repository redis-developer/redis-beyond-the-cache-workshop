package com.redis.workshop.platform.executionplane.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
class ExecutionPlaneSecurityConfiguration {

    static final String EXECUTION_PLANE_KEY_HEADER = "X-Execution-Plane-Key";
    private static final String INTERNAL_API_PREFIX = "/internal/execution-plane/";

    @Bean
    SecurityFilterChain executionPlaneSecurityFilterChain(
        HttpSecurity http,
        @Value("${platform.execution-plane.internal-api.shared-secret:${EXECUTION_PLANE_SHARED_SECRET:}}")
        String sharedSecret
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().permitAll()
            )
            .addFilterBefore(
                new ExecutionPlaneSharedSecretFilter(sharedSecret),
                UsernamePasswordAuthenticationFilter.class
            );
        return http.build();
    }

    private static final class ExecutionPlaneSharedSecretFilter extends OncePerRequestFilter {

        private final byte[] expectedSecret;

        private ExecutionPlaneSharedSecretFilter(String sharedSecret) {
            this.expectedSecret = normalize(sharedSecret).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
        ) throws ServletException, IOException {
            if (!isInternalApiRequest(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String providedSecret = request.getHeader(EXECUTION_PLANE_KEY_HEADER);
            if (!matches(providedSecret)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            filterChain.doFilter(request, response);
        }

        private boolean isInternalApiRequest(HttpServletRequest request) {
            return request.getRequestURI().startsWith(INTERNAL_API_PREFIX);
        }

        private boolean matches(String providedSecret) {
            byte[] provided = normalize(providedSecret).getBytes(StandardCharsets.UTF_8);
            return expectedSecret.length > 0 && MessageDigest.isEqual(expectedSecret, provided);
        }

        private static String normalize(String value) {
            return value == null ? "" : value;
        }
    }
}

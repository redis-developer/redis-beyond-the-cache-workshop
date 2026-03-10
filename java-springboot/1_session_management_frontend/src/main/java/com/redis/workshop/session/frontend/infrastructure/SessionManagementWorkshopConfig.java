package com.redis.workshop.session.frontend.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Session Management Workshop Configuration
 * 
 * This is the specific configuration for the "Session Management with Redis" workshop.
 * It defines which files can be edited and their original content.
 * 
 * To create a new workshop, simply create a new class implementing WorkshopConfig
 * with different files and content.
 */
@Component
public class SessionManagementWorkshopConfig implements WorkshopConfig {

    private static final Map<String, String> EDITABLE_FILES = Map.of(
        "build.gradle.kts", "build.gradle.kts",
        "application.properties", "src/main/resources/application.properties",
        "SecurityConfig.java", "src/main/java/com/redis/workshop/session/config/SecurityConfig.java"
    );

    private static final Map<String, String> ORIGINAL_CONTENTS = Map.of(
        "build.gradle.kts", """
            plugins {
                java
                id("org.springframework.boot")
                id("io.spring.dependency-management")
            }
            
            group = "com.redis.workshop"
            version = "0.0.1-SNAPSHOT"
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation(project(":workshop-infrastructure"))

                implementation("org.springframework.boot:spring-boot-starter-web")
                implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
                implementation("org.springframework.boot:spring-boot-starter-security")

                // TODO: Uncomment the lines below to enable Redis session management
                // implementation("org.springframework.boot:spring-boot-starter-data-redis")
                // implementation("org.springframework.session:spring-session-data-redis")

                testImplementation("org.springframework.boot:spring-boot-starter-test")
                testImplementation("org.springframework.security:spring-security-test")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }
            
            tasks.withType<Test> {
                useJUnitPlatform()
            }
            """,
        
        "application.properties", """
            # Application name
            spring.application.name=session-management
            
            # Server configuration
            server.port=${SERVER_PORT:8080}
            
            # Redis configuration
            spring.data.redis.host=localhost
            spring.data.redis.port=6379
            
            # Session configuration
            # TODO: Change 'none' to 'redis' to enable Redis session storage
            spring.session.store-type=none
            #spring.session.redis.namespace=spring:session
            #spring.session.redis.flush-mode=immediate
            #spring.session.redis.repository-type=default
            
            # Session timeout (30 minutes)
            server.servlet.session.timeout=30m
            
            # Logging
            logging.level.org.springframework.session=DEBUG
            """,
        
        "SecurityConfig.java", """
            package com.redis.workshop.session.config;

            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.security.config.annotation.web.builders.HttpSecurity;
            import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
            import org.springframework.security.core.userdetails.User;
            import org.springframework.security.core.userdetails.UserDetails;
            import org.springframework.security.core.userdetails.UserDetailsService;
            import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
            import org.springframework.security.crypto.password.PasswordEncoder;
            import org.springframework.security.provisioning.InMemoryUserDetailsManager;
            import org.springframework.security.web.SecurityFilterChain;
            // TODO: Uncomment the imports below to enable session persistence
            // import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
            // import org.springframework.security.web.context.SecurityContextRepository;
            import org.springframework.security.web.session.HttpSessionEventPublisher;
            import org.springframework.http.HttpStatus;

            @Configuration
            @EnableWebSecurity
            public class SecurityConfig {

                @Bean
                public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                    http
                        // TODO: Uncomment the lines below to store SecurityContext in HTTP session
                        // .securityContext(context -> context
                        //     .securityContextRepository(securityContextRepository())
                        // )
                        .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                            .requestMatchers("/login").permitAll()
                            .anyRequest().authenticated()
                        )
                        // Return 401 for unauthenticated API requests instead of redirecting to login page
                        .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((request, response, authException) -> {
                                if (request.getRequestURI().startsWith("/api/")) {
                                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\\"error\\":\\"Unauthorized\\"}");
                                } else {
                                    response.sendRedirect("/login");
                                }
                            })
                        )
                        .formLogin(form -> form
                            .loginPage("/login")
                            .loginProcessingUrl("/login")
                            .defaultSuccessUrl("/welcome", true)
                            .permitAll()
                        )
                        .logout(logout -> logout
                            .logoutSuccessUrl("/login?logout")
                            .permitAll()
                        )
                        .csrf(csrf -> csrf.disable());

                    return http.build();
                }

                // TODO: Uncomment the bean below to enable session-based security context
                // @Bean
                // public SecurityContextRepository securityContextRepository() {
                //     return new HttpSessionSecurityContextRepository();
                // }

                @Bean
                public UserDetailsService userDetailsService() {
                    UserDetails user = User.builder()
                            .username("user")
                            .password(passwordEncoder().encode("password"))
                            .roles("USER")
                            .build();

                    return new InMemoryUserDetailsManager(user);
                }

                @Bean
                public PasswordEncoder passwordEncoder() {
                    return new BCryptPasswordEncoder();
                }

                @Bean
                public HttpSessionEventPublisher httpSessionEventPublisher() {
                    return new HttpSessionEventPublisher();
                }
            }
            """
    );

    @Override
    public Map<String, String> getEditableFiles() {
        return EDITABLE_FILES;
    }

    @Override
    public String getOriginalContent(String fileName) {
        return ORIGINAL_CONTENTS.get(fileName);
    }

    @Override
    public String getModuleName() {
        return "1_session_management";
    }

    @Override
    public String getWorkshopTitle() {
        return "Distributed Session Management with Redis";
    }

    @Override
    public String getWorkshopDescription() {
        return "Learn how to use Redis for distributed session management in Spring Boot applications";
    }
}

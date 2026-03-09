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
                        response.getWriter().write("{\"error\":\"Unauthorized\"}");
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

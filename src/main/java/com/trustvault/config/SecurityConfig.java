package com.trustvault.config;

import com.trustvault.entity.AuditOutcome;
import com.trustvault.security.CustomUserDetails;
import com.trustvault.security.CustomUserDetailsService;
import com.trustvault.service.TamperEvidentAuditService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TamperEvidentAuditService auditService;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder,
                          TamperEvidentAuditService auditService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/login",
                    "/register",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/actuator/health",
                    "/error",
                    "/access-denied"
                ).permitAll()
                .requestMatchers("/admin/**", "/api/admin/**").hasAuthority("ROLE_ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "ANONYMOUS";
                    auditService.recordEvent(
                        null,
                        username,
                        "UNKNOWN",
                        "UNAUTHORIZED_ACCESS_ATTEMPT",
                        request.getRequestURI(),
                        "HTTP_ENDPOINT",
                        AuditOutcome.DENIED,
                        "Access denied to resource: " + request.getRequestURI()
                    );
                    response.sendRedirect("/access-denied");
                })
            )
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(2)
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                auditService.recordEvent(
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getUser().getRole().name(),
                    "USER_LOGIN",
                    String.valueOf(userDetails.getId()),
                    "USER",
                    AuditOutcome.SUCCESS,
                    "User authenticated successfully."
                );
            }
            response.sendRedirect("/dashboard");
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String attemptedUser = request.getParameter("username");
            auditService.recordEvent(
                null,
                attemptedUser != null ? attemptedUser : "UNKNOWN",
                "NONE",
                "FAILED_LOGIN_ATTEMPT",
                "AUTH",
                "USER",
                AuditOutcome.FAILURE,
                "Authentication failure: " + exception.getMessage()
            );
            response.sendRedirect("/login?error=true");
        };
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                auditService.recordEvent(
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getUser().getRole().name(),
                    "USER_LOGOUT",
                    String.valueOf(userDetails.getId()),
                    "USER",
                    AuditOutcome.SUCCESS,
                    "User logged out successfully."
                );
            }
            response.sendRedirect("/login?logout=true");
        };
    }
}

package com.provert.payroll.security.config;

import com.provert.payroll.security.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: " + authException.getMessage())
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()

                        // Finance Manager exclusive controls
                        .requestMatchers("/api/overtime/calculate", "/api/overtime/*/approve").hasAuthority("ROLE_FINANCE_MANAGER")
                        .requestMatchers("/api/payslips/generate", "/api/payslips/*/finalize").hasAuthority("ROLE_FINANCE_MANAGER")

                        // HR Admin exclusive config controls
                        .requestMatchers(HttpMethod.POST, "/api/working-hour-rules/**", "/api/holidays/**").hasAuthority("ROLE_HR_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/working-hour-rules/**", "/api/holidays/**").hasAuthority("ROLE_HR_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/working-hour-rules/**", "/api/holidays/**").hasAuthority("ROLE_HR_ADMIN")

                        // HR Admin & Finance Manager shared master data management
                        .requestMatchers("/api/departments/**", "/api/employees/**").hasAnyAuthority("ROLE_HR_ADMIN", "ROLE_FINANCE_MANAGER")

                        // Attendance capture
                        .requestMatchers(HttpMethod.POST, "/api/attendance/**").hasAnyAuthority("ROLE_HR_ADMIN", "ROLE_EMPLOYEE", "ROLE_FINANCE_MANAGER")

                        // Any other API request requires authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

package com.example.notificationservice.config;

import com.example.common.security.JwtAuthenticationFilter;
import com.example.common.security.JwtPrincipal;
import com.example.common.security.JwtService;
import com.example.common.security.RestSecurityHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(antMatcher("/actuator/health/**"), antMatcher("/actuator/info")).permitAll()
                        // Only other microservices (service JWT) may queue emails.
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/notifications/email"))
                        .hasRole(JwtPrincipal.ROLE_SERVICE)
                        .requestMatchers(antMatcher("/api/notifications/admin/**")).hasRole(JwtPrincipal.ROLE_ADMIN)
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(RestSecurityHandlers.authenticationEntryPoint())
                        .accessDeniedHandler(RestSecurityHandlers.accessDeniedHandler()))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

package com.example.tutorservice.config;

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
import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

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
                        // Public browsing: anyone can search and view tutor profiles.
                        .requestMatchers(
                                antMatcher(HttpMethod.GET, "/api/tutors/search"),
                                antMatcher(HttpMethod.GET, "/api/tutors/subjects"),
                                antMatcher(HttpMethod.GET, "/api/tutors/user/*"),
                                regexMatcher(HttpMethod.GET, "^/api/tutors/\\d+(\\?.*)?$")).permitAll()
                        // Service-to-service API (contains tutor email) - only callable with a service token.
                        .requestMatchers(antMatcher("/api/tutors/internal/**")).hasRole(JwtPrincipal.ROLE_SERVICE)
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(RestSecurityHandlers.authenticationEntryPoint())
                        .accessDeniedHandler(RestSecurityHandlers.accessDeniedHandler()))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

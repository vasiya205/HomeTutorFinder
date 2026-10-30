package com.example.authservice.config;

import com.example.authservice.repository.UserRepository;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   UserRepository userRepository) throws Exception {
        // A disabled account loses access immediately, even with a token that has not expired yet.
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, principal ->
                principal.isService() || userRepository.findById(principal.userId())
                        .map(user -> user.isEnabled())
                        .orElse(false));

        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(antMatcher("/actuator/health/**"), antMatcher("/actuator/info")).permitAll()
                        .requestMatchers(
                                antMatcher(HttpMethod.POST, "/api/auth/register"),
                                antMatcher(HttpMethod.POST, "/api/auth/login"),
                                antMatcher(HttpMethod.POST, "/api/auth/forgot-password"),
                                antMatcher(HttpMethod.POST, "/api/auth/reset-password")).permitAll()
                        .requestMatchers(antMatcher("/api/auth/admin/**")).hasRole(JwtPrincipal.ROLE_ADMIN)
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(RestSecurityHandlers.authenticationEntryPoint())
                        .accessDeniedHandler(RestSecurityHandlers.accessDeniedHandler()))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

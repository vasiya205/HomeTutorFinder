package com.example.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * Reads "Authorization: Bearer &lt;jwt&gt;" and, when the token is valid, puts a
 * {@link JwtPrincipal} into the Spring Security context with authority ROLE_&lt;role&gt;.
 * Not a Spring bean on purpose: each service registers it inside its SecurityFilterChain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final Predicate<JwtPrincipal> principalCheck;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this(jwtService, principal -> true);
    }

    /**
     * @param principalCheck extra check, e.g. the auth-service verifies the account is still enabled
     */
    public JwtAuthenticationFilter(JwtService jwtService, Predicate<JwtPrincipal> principalCheck) {
        this.jwtService = jwtService;
        this.principalCheck = principalCheck;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            jwtService.parse(token)
                    .filter(principalCheck)
                    .ifPresent(principal -> {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }
}

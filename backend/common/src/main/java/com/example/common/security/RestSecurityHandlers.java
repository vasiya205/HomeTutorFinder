package com.example.common.security;

import com.example.common.web.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/** JSON responses for 401/403 raised by the security filter chain (before controllers run). */
public final class RestSecurityHandlers {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RestSecurityHandlers() {}

    public static AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> write(response, HttpStatus.UNAUTHORIZED,
                "Please sign in to continue.", request.getRequestURI());
    }

    public static AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> write(response, HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action.", request.getRequestURI());
    }

    private static void write(HttpServletResponse response, HttpStatus status, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getOutputStream(), ApiError.of(status, message, path));
    }
}

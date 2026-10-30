package com.example.authservice.api;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserSummary user
) {}

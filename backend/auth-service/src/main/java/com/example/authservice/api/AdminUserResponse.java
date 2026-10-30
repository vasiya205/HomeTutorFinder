package com.example.authservice.api;

import com.example.authservice.domain.UserEntity;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        String role,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    public static AdminUserResponse from(UserEntity user) {
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
                user.isEnabled(), user.getCreatedAt(), user.getLastLoginAt());
    }
}

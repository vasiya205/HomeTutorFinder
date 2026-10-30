package com.example.authservice.api;

import com.example.authservice.domain.UserEntity;

import java.time.LocalDateTime;

public record UserSummary(
        Long id,
        String fullName,
        String email,
        String role,
        LocalDateTime createdAt
) {
    public static UserSummary from(UserEntity user) {
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
                user.getCreatedAt());
    }
}

package com.example.tutorservice.api;

import com.example.tutorservice.domain.TutorProfileEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Public view of a tutor (no email address). */
public record TutorProfileResponse(
        Long id,
        Long userId,
        String fullName,
        String qualification,
        String subject,
        String location,
        Integer experienceYears,
        String bio,
        BigDecimal hourlyRate,
        String teachingMode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TutorProfileResponse from(TutorProfileEntity profile) {
        return new TutorProfileResponse(profile.getId(), profile.getUserId(), profile.getFullName(),
                profile.getQualification(), profile.getSubject(), profile.getLocation(), profile.getExperienceYears(),
                profile.getBio(), profile.getHourlyRate(), profile.getTeachingMode().name(),
                profile.getCreatedAt(), profile.getUpdatedAt());
    }
}

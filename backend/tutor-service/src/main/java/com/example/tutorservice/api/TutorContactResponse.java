package com.example.tutorservice.api;

import com.example.tutorservice.domain.TutorProfileEntity;

import java.math.BigDecimal;

/** Internal view used by booking-service: includes the tutor email for notifications. */
public record TutorContactResponse(
        Long userId,
        String fullName,
        String email,
        String subject,
        BigDecimal hourlyRate
) {
    public static TutorContactResponse from(TutorProfileEntity profile) {
        return new TutorContactResponse(profile.getUserId(), profile.getFullName(), profile.getEmail(),
                profile.getSubject(), profile.getHourlyRate());
    }
}

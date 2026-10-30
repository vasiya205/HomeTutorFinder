package com.example.bookingservice.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BookingRequest(
        @NotNull(message = "Please choose a tutor") Long tutorUserId,

        @NotBlank(message = "Subject is required")
        @Size(max = 80, message = "Subject must be at most 80 characters")
        String subject,

        @NotNull(message = "Start time is required") LocalDateTime startDateTime,

        @NotNull(message = "End time is required") LocalDateTime endDateTime,

        @Size(max = 1000, message = "Notes must be at most 1000 characters")
        String notes
) {}

package com.example.tutorservice.api;

import com.example.tutorservice.domain.TeachingMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** The tutor's name and email come from their JWT, so they are not part of the request. */
public record TutorProfileRequest(
        @NotBlank(message = "Qualification is required")
        @Size(max = 150, message = "Qualification must be at most 150 characters")
        String qualification,

        @NotBlank(message = "Subject is required")
        @Size(max = 80, message = "Subject must be at most 80 characters")
        String subject,

        @NotBlank(message = "Location is required")
        @Size(max = 80, message = "Location must be at most 80 characters")
        String location,

        @NotNull(message = "Experience is required")
        @Min(value = 0, message = "Experience cannot be negative")
        @Max(value = 60, message = "Experience must be 60 years or less")
        Integer experienceYears,

        @NotBlank(message = "Please write a short bio")
        @Size(min = 20, max = 2000, message = "Bio must be between 20 and 2000 characters")
        String bio,

        @NotNull(message = "Hourly rate is required")
        @DecimalMin(value = "100.00", message = "Hourly rate must be at least ₹100")
        @DecimalMax(value = "10000.00", message = "Hourly rate must be at most ₹10,000")
        BigDecimal hourlyRate,

        TeachingMode teachingMode
) {}

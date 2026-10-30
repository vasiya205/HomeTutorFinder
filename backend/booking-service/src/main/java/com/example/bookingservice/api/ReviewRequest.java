package com.example.bookingservice.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull(message = "bookingId is required") Long bookingId,

        @NotNull(message = "Please choose a rating")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Integer rating,

        @NotBlank(message = "Please write a short comment")
        @Size(min = 3, max = 1000, message = "Comment must be between 3 and 1000 characters")
        String comment
) {}

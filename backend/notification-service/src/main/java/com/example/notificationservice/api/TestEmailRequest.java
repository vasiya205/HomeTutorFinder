package com.example.notificationservice.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TestEmailRequest(
        @NotBlank(message = "Recipient email is required") @Email(message = "Enter a valid email address") String to
) {}

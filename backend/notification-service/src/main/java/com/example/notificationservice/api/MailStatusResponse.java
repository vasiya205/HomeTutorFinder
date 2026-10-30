package com.example.notificationservice.api;

public record MailStatusResponse(
        boolean smtpConfigured,
        String fromAddress,
        long total,
        long sent,
        long logged,
        long failed
) {}

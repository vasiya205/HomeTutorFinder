package com.example.notificationservice.api;

import com.example.notificationservice.domain.EmailNotificationEntity;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailNotificationResponse(
        Long id,
        String type,
        String recipientEmail,
        String recipientName,
        String subject,
        String status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime sentAt,
        String htmlBody
) {
    public static EmailNotificationResponse summary(EmailNotificationEntity email) {
        return of(email, false);
    }

    public static EmailNotificationResponse full(EmailNotificationEntity email) {
        return of(email, true);
    }

    private static EmailNotificationResponse of(EmailNotificationEntity email, boolean withBody) {
        return new EmailNotificationResponse(email.getId(), email.getType(), email.getRecipientEmail(),
                email.getRecipientName(), email.getSubject(), email.getStatus().name(), email.getErrorMessage(),
                email.getCreatedAt(), email.getSentAt(), withBody ? email.getHtmlBody() : null);
    }
}

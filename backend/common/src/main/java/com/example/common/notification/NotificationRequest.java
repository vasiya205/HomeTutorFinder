package com.example.common.notification;

import java.util.HashMap;
import java.util.Map;

/**
 * Payload sent to the notification-service. {@code data} holds the template variables,
 * e.g. {"subject": "Mathematics", "amount": "800.00"}.
 */
public record NotificationRequest(
        NotificationType type,
        String recipientEmail,
        String recipientName,
        Map<String, String> data
) {
    public NotificationRequest {
        Map<String, String> clean = new HashMap<>();
        if (data != null) {
            data.forEach((key, value) -> {
                if (key != null && value != null) {
                    clean.put(key, value);
                }
            });
        }
        data = Map.copyOf(clean);
    }
}

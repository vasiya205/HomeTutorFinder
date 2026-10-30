package com.example.notificationservice.service;

import com.example.common.notification.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplateRendererTest {
    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer("http://localhost:5173");

    @Test
    void everyNotificationTypeRenders() {
        Map<String, String> data = Map.of("subject", "Mathematics", "tutorName", "Priya Sharma",
                "studentName", "Sneha Patel", "date", "Sat, 31 Oct 2026", "time", "5:00 PM - 6:00 PM",
                "duration", "60 minutes", "amount", "₹800.00", "bookingId", "7", "paymentId", "pay_123");
        for (NotificationType type : NotificationType.values()) {
            RenderedEmail email = renderer.render(type, "Sneha Patel", data);
            assertFalse(email.subject().isBlank(), type + " subject");
            assertTrue(email.html().contains("Home Tutor Finder"), type + " html");
        }
    }

    @Test
    void escapesUserSuppliedValues() {
        RenderedEmail email = renderer.render(NotificationType.BOOKING_REQUESTED_TUTOR, "Priya",
                Map.of("studentName", "<script>alert(1)</script>", "subject", "Maths", "notes", "a & b"));
        assertFalse(email.html().contains("<script>"));
        assertTrue(email.html().contains("&lt;script&gt;"));
    }

    @Test
    void plainTextVersionHasNoTags() {
        RenderedEmail email = renderer.render(NotificationType.WELCOME, "Sneha", Map.of("role", "STUDENT"));
        String text = EmailDispatcher.toPlainText(email.html());
        assertFalse(text.contains("<"));
        assertTrue(text.contains("Welcome aboard"));
    }
}

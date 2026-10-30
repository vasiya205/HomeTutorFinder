package com.example.common.notification;

/** Every email the platform sends. The notification-service owns the HTML template for each one. */
public enum NotificationType {
    WELCOME,
    PASSWORD_RESET,
    PASSWORD_CHANGED,
    BOOKING_REQUESTED_TUTOR,
    BOOKING_REQUESTED_STUDENT,
    BOOKING_ACCEPTED,
    BOOKING_REJECTED,
    BOOKING_CANCELLED,
    BOOKING_COMPLETED,
    PAYMENT_RECEIPT,
    PAYMENT_RECEIVED,
    TEST
}

package com.example.bookingservice.domain;

/**
 * PENDING -> ACCEPTED -> (paid) -> COMPLETED
 * PENDING -> REJECTED,  PENDING/ACCEPTED (unpaid) -> CANCELLED
 */
public enum BookingStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED;

    public boolean isActive() {
        return this == PENDING || this == ACCEPTED;
    }
}

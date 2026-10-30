package com.example.bookingservice.api;

public record PaymentResultResponse(
        String paymentStatus,
        String paymentId,
        String mode,
        String message,
        BookingResponse booking
) {}

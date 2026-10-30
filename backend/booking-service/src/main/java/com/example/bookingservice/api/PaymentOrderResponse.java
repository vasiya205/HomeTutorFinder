package com.example.bookingservice.api;

import java.math.BigDecimal;

/**
 * What the browser needs to open the checkout.
 * mode = RAZORPAY: open Razorpay Checkout with keyId + orderId.
 * mode = DEMO: show the built-in demo checkout (no keys configured).
 */
public record PaymentOrderResponse(
        Long bookingId,
        String mode,
        String keyId,
        String orderId,
        long amountInPaise,
        String currency,
        BigDecimal totalAmount,
        String description,
        String customerName,
        String customerEmail
) {}

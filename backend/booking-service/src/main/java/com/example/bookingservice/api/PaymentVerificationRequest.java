package com.example.bookingservice.api;

import jakarta.validation.constraints.NotBlank;

/** For RAZORPAY payments paymentId and signature come from Razorpay Checkout; DEMO payments only need orderId. */
public record PaymentVerificationRequest(
        @NotBlank(message = "orderId is required") String orderId,
        String paymentId,
        String signature
) {}

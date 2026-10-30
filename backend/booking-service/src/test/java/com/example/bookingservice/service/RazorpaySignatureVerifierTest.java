package com.example.bookingservice.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RazorpaySignatureVerifierTest {
    @Test
    void acceptsSignatureForMatchingOrderAndPayment() throws Exception {
        String secret = "rotated-test-secret";
        String orderId = "order_demo_123";
        String paymentId = "pay_demo_456";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(
                mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));

        assertTrue(RazorpaySignatureVerifier.isValid(secret, orderId, paymentId, signature));
        assertFalse(RazorpaySignatureVerifier.isValid(secret, "different-order", paymentId, signature));
        assertFalse(RazorpaySignatureVerifier.isValid("wrong-secret", orderId, paymentId, signature));
    }
}

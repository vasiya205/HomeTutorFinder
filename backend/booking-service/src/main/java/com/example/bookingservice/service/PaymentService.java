package com.example.bookingservice.service;

import com.example.bookingservice.api.BookingResponse;
import com.example.bookingservice.api.PaymentOrderResponse;
import com.example.bookingservice.api.PaymentResultResponse;
import com.example.bookingservice.api.PaymentVerificationRequest;
import com.example.bookingservice.domain.BookingEntity;
import com.example.bookingservice.domain.BookingStatus;
import com.example.bookingservice.domain.PaymentMode;
import com.example.bookingservice.domain.PaymentStatus;
import com.example.bookingservice.repository.BookingRepository;
import com.example.common.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Lesson payments.
 * RAZORPAY: creates a Razorpay test order and verifies the HMAC signature returned by Checkout.
 * DEMO: a simulated checkout for when no Razorpay keys are configured (clearly labelled in the UI).
 * In both cases the amount comes from the booking stored on the server - never from the browser.
 */
@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final BookingRepository bookingRepository;
    private final BookingNotifier notifier;
    private final RestClient razorpayClient;
    private final String keyId;
    private final String keySecret;
    private final String configuredMode;
    private final String currency;

    public PaymentService(BookingRepository bookingRepository,
                          BookingNotifier notifier,
                          RestClient.Builder restClientBuilder,
                          @Value("${payment.razorpay.key-id:}") String keyId,
                          @Value("${payment.razorpay.key-secret:}") String keySecret,
                          @Value("${payment.mode:AUTO}") String configuredMode,
                          @Value("${payment.currency:INR}") String currency) {
        this.bookingRepository = bookingRepository;
        this.notifier = notifier;
        this.razorpayClient = restClientBuilder.baseUrl("https://api.razorpay.com/v1").build();
        this.keyId = keyId == null ? "" : keyId.trim();
        this.keySecret = keySecret == null ? "" : keySecret.trim();
        this.configuredMode = configuredMode == null ? "AUTO" : configuredMode.trim().toUpperCase(Locale.ROOT);
        this.currency = currency;
        if (!this.keyId.isEmpty() && !this.keyId.startsWith("rzp_test_")) {
            log.warn("RAZORPAY_KEY_ID is not a test key (rzp_test_...). Live keys are refused; falling back to DEMO payments.");
        }
        log.info("Payment mode: {}", activeMode());
    }

    public PaymentMode activeMode() {
        boolean razorpayReady = keyId.startsWith("rzp_test_") && !keySecret.isEmpty();
        return switch (configuredMode) {
            case "DEMO" -> PaymentMode.DEMO;
            case "RAZORPAY" -> razorpayReady ? PaymentMode.RAZORPAY : PaymentMode.DEMO;
            default -> razorpayReady ? PaymentMode.RAZORPAY : PaymentMode.DEMO;
        };
    }

    @Transactional
    public PaymentOrderResponse createOrder(Long studentUserId, Long bookingId) {
        BookingEntity booking = findStudentBooking(studentUserId, bookingId);
        requirePayable(booking);
        PaymentMode mode = activeMode();

        // Re-use an open order (e.g. the student closed the checkout and clicked Pay again).
        if (booking.getPaymentStatus() == PaymentStatus.PENDING && booking.getPaymentOrderId() != null
                && booking.getPaymentMode() == mode) {
            return toOrderResponse(booking);
        }

        String orderId = mode == PaymentMode.RAZORPAY
                ? createRazorpayOrder(booking)
                : "demo_order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        booking.setPaymentMode(mode);
        booking.setPaymentOrderId(orderId);
        booking.setPaymentId(null);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        return toOrderResponse(bookingRepository.save(booking));
    }

    @Transactional
    public PaymentResultResponse verifyPayment(Long studentUserId, Long bookingId, PaymentVerificationRequest request) {
        BookingEntity booking = findStudentBooking(studentUserId, bookingId);
        if (booking.isPaid()) {
            return result(booking, "This lesson is already paid.");
        }
        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw ApiException.conflict("Only accepted lessons can be paid.");
        }
        if (booking.getPaymentStatus() != PaymentStatus.PENDING
                || !request.orderId().equals(booking.getPaymentOrderId())) {
            throw ApiException.badRequest("This payment does not match the booking. Please start the payment again.");
        }

        String paymentId;
        if (booking.getPaymentMode() == PaymentMode.RAZORPAY) {
            if (!RazorpaySignatureVerifier.isValid(keySecret, request.orderId(), request.paymentId(), request.signature())) {
                throw ApiException.badRequest("Payment signature is invalid. The payment was not recorded.");
            }
            paymentId = request.paymentId();
        } else {
            paymentId = "demo_pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        booking.setPaymentId(paymentId);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setPaidAt(LocalDateTime.now());
        BookingEntity saved = bookingRepository.save(booking);
        notifier.paymentSucceeded(saved);
        return result(saved, saved.getPaymentMode() == PaymentMode.RAZORPAY
                ? "Payment verified with Razorpay (test mode). A receipt has been emailed to you."
                : "Demo payment completed. A receipt has been emailed to you.");
    }

    private String createRazorpayOrder(BookingEntity booking) {
        RazorpayOrder order;
        try {
            order = razorpayClient.post()
                    .uri("/orders")
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                    .body(Map.of(
                            "amount", amountInPaise(booking),
                            "currency", currency,
                            "receipt", "booking-" + booking.getId(),
                            "notes", Map.of("bookingId", String.valueOf(booking.getId()))))
                    .retrieve()
                    .body(RazorpayOrder.class);
        } catch (RestClientException exception) {
            log.warn("Razorpay order creation failed: {}", exception.getMessage());
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Could not create the Razorpay order. Check RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET.");
        }
        if (order == null || order.id() == null || order.id().isBlank()) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, "Razorpay returned an invalid order.");
        }
        return order.id();
    }

    private BookingEntity findStudentBooking(Long studentUserId, Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!studentUserId.equals(booking.getStudentUserId())) {
            throw ApiException.forbidden("You can only pay for your own bookings.");
        }
        return booking;
    }

    private static void requirePayable(BookingEntity booking) {
        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw ApiException.conflict("You can pay once the tutor has accepted the request.");
        }
        if (booking.isPaid()) {
            throw ApiException.conflict("This lesson is already paid.");
        }
        if (booking.getTotalAmount().signum() <= 0) {
            throw ApiException.badRequest("This lesson is free - no payment needed.");
        }
    }

    private PaymentOrderResponse toOrderResponse(BookingEntity booking) {
        return new PaymentOrderResponse(
                booking.getId(),
                booking.getPaymentMode().name(),
                booking.getPaymentMode() == PaymentMode.RAZORPAY ? keyId : null,
                booking.getPaymentOrderId(),
                amountInPaise(booking),
                currency,
                booking.getTotalAmount(),
                booking.getSubject() + " lesson with " + booking.getTutorName(),
                booking.getStudentName(),
                booking.getStudentEmail());
    }

    private static long amountInPaise(BookingEntity booking) {
        return booking.getTotalAmount().setScale(2, java.math.RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    private static PaymentResultResponse result(BookingEntity booking, String message) {
        return new PaymentResultResponse(
                booking.getPaymentStatus().name(),
                booking.getPaymentId(),
                booking.getPaymentMode() == null ? null : booking.getPaymentMode().name(),
                message,
                BookingResponse.from(booking, false));
    }

    private record RazorpayOrder(String id) {}
}

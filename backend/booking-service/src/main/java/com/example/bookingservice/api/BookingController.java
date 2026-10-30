package com.example.bookingservice.api;

import com.example.bookingservice.service.BookingService;
import com.example.bookingservice.service.PaymentService;
import com.example.common.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final PaymentService paymentService;

    public BookingController(BookingService bookingService, PaymentService paymentService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal JwtPrincipal student,
                                                  @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(student, request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal JwtPrincipal student) {
        return bookingService.listStudentBookings(student.userId());
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasRole('TUTOR')")
    public List<BookingResponse> incoming(@AuthenticationPrincipal JwtPrincipal tutor) {
        return bookingService.listTutorBookings(tutor.userId());
    }

    /** Tutor: ACCEPTED / REJECTED / COMPLETED / CANCELLED. Student: CANCELLED. */
    @PatchMapping("/{id}/status")
    public BookingResponse updateStatus(@AuthenticationPrincipal JwtPrincipal principal,
                                        @PathVariable Long id,
                                        @Valid @RequestBody BookingStatusRequest request) {
        return bookingService.updateStatus(principal, id, request.status());
    }

    @PostMapping("/{id}/payments/order")
    @PreAuthorize("hasRole('STUDENT')")
    public PaymentOrderResponse createPaymentOrder(@AuthenticationPrincipal JwtPrincipal student,
                                                   @PathVariable Long id) {
        return paymentService.createOrder(student.userId(), id);
    }

    @PostMapping("/{id}/payments/verify")
    @PreAuthorize("hasRole('STUDENT')")
    public PaymentResultResponse verifyPayment(@AuthenticationPrincipal JwtPrincipal student,
                                               @PathVariable Long id,
                                               @Valid @RequestBody PaymentVerificationRequest request) {
        return paymentService.verifyPayment(student.userId(), id, request);
    }
}

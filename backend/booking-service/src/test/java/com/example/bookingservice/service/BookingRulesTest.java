package com.example.bookingservice.service;

import com.example.bookingservice.domain.BookingEntity;
import com.example.bookingservice.domain.BookingStatus;
import com.example.bookingservice.domain.PaymentStatus;
import com.example.common.web.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingRulesTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 30, 9, 0);

    @Test
    void calculatesTotalForOneHour() {
        assertEquals(new BigDecimal("800.00"),
                BookingService.calculateTotalAmount(new BigDecimal("800.00"), NOW, NOW.plusHours(1)));
    }

    @Test
    void calculatesAndRoundsPartialHourToPaise() {
        assertEquals(new BigDecimal("50.01"),
                BookingService.calculateTotalAmount(new BigDecimal("100.01"), NOW, NOW.plusMinutes(30)));
    }

    @Test
    void rejectsLessonsInThePastOrWithBadDuration() {
        assertThrows(ApiException.class, () -> BookingService.validateTimes(NOW.minusHours(1), NOW, NOW));
        assertThrows(ApiException.class, () -> BookingService.validateTimes(NOW.plusHours(1), NOW.plusHours(1).plusMinutes(10), NOW));
        assertThrows(ApiException.class, () -> BookingService.validateTimes(NOW.plusHours(1), NOW.plusHours(6), NOW));
        assertDoesNotThrow(() -> BookingService.validateTimes(NOW.plusDays(1), NOW.plusDays(1).plusHours(1), NOW));
    }

    @Test
    void onlyTutorCanAcceptPendingRequests() {
        BookingEntity booking = booking(BookingStatus.PENDING, PaymentStatus.UNPAID);
        assertThrows(ApiException.class, () -> BookingService.validateTransition(true, false, booking, BookingStatus.ACCEPTED));
        assertDoesNotThrow(() -> BookingService.validateTransition(false, true, booking, BookingStatus.ACCEPTED));
    }

    @Test
    void cancelledBookingCannotBeAccepted() {
        BookingEntity booking = booking(BookingStatus.CANCELLED, PaymentStatus.UNPAID);
        assertThrows(ApiException.class, () -> BookingService.validateTransition(false, true, booking, BookingStatus.ACCEPTED));
    }

    @Test
    void lessonMustBePaidBeforeItIsCompleted() {
        BookingEntity unpaid = booking(BookingStatus.ACCEPTED, PaymentStatus.UNPAID);
        assertThrows(ApiException.class, () -> BookingService.validateTransition(false, true, unpaid, BookingStatus.COMPLETED));
        BookingEntity paid = booking(BookingStatus.ACCEPTED, PaymentStatus.PAID);
        assertDoesNotThrow(() -> BookingService.validateTransition(false, true, paid, BookingStatus.COMPLETED));
    }

    @Test
    void paidLessonCannotBeCancelledOnline() {
        BookingEntity paid = booking(BookingStatus.ACCEPTED, PaymentStatus.PAID);
        assertThrows(ApiException.class, () -> BookingService.validateTransition(true, false, paid, BookingStatus.CANCELLED));
        BookingEntity unpaid = booking(BookingStatus.ACCEPTED, PaymentStatus.UNPAID);
        assertDoesNotThrow(() -> BookingService.validateTransition(true, false, unpaid, BookingStatus.CANCELLED));
    }

    @Test
    void strangersCannotChangeABooking() {
        BookingEntity booking = booking(BookingStatus.PENDING, PaymentStatus.UNPAID);
        assertThrows(ApiException.class, () -> BookingService.validateTransition(false, false, booking, BookingStatus.CANCELLED));
    }

    private static BookingEntity booking(BookingStatus status, PaymentStatus paymentStatus) {
        BookingEntity booking = new BookingEntity();
        booking.setStatus(status);
        booking.setPaymentStatus(paymentStatus);
        booking.setTotalAmount(new BigDecimal("800.00"));
        booking.setStartDateTime(NOW.plusDays(1));
        booking.setEndDateTime(NOW.plusDays(1).plusHours(1));
        return booking;
    }
}

package com.example.bookingservice.api;

import com.example.bookingservice.domain.BookingEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long studentUserId,
        String studentName,
        Long tutorUserId,
        String tutorName,
        String subject,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        long durationMinutes,
        String notes,
        BigDecimal hourlyRate,
        BigDecimal totalAmount,
        String status,
        String paymentStatus,
        String paymentMode,
        String paymentId,
        LocalDateTime paidAt,
        boolean reviewed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BookingResponse from(BookingEntity booking, boolean reviewed) {
        return new BookingResponse(
                booking.getId(),
                booking.getStudentUserId(),
                booking.getStudentName(),
                booking.getTutorUserId(),
                booking.getTutorName(),
                booking.getSubject(),
                booking.getStartDateTime(),
                booking.getEndDateTime(),
                booking.durationMinutes(),
                booking.getNotes(),
                booking.getHourlyRate(),
                booking.getTotalAmount(),
                booking.getStatus().name(),
                booking.getPaymentStatus().name(),
                booking.getPaymentMode() == null ? null : booking.getPaymentMode().name(),
                booking.getPaymentId(),
                booking.getPaidAt(),
                reviewed,
                booking.getCreatedAt(),
                booking.getUpdatedAt());
    }
}

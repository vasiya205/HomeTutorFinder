package com.example.bookingservice.api;

import com.example.bookingservice.domain.ReviewEntity;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long bookingId,
        String studentName,
        Long tutorUserId,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(ReviewEntity review) {
        return new ReviewResponse(review.getId(), review.getBookingId(), review.getStudentName(),
                review.getTutorUserId(), review.getRating(), review.getComment(), review.getCreatedAt());
    }
}

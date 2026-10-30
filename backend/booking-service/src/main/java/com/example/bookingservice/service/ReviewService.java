package com.example.bookingservice.service;

import com.example.bookingservice.api.RatingSummaryResponse;
import com.example.bookingservice.api.ReviewRequest;
import com.example.bookingservice.api.ReviewResponse;
import com.example.bookingservice.domain.BookingEntity;
import com.example.bookingservice.domain.BookingStatus;
import com.example.bookingservice.domain.ReviewEntity;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.ReviewRepository;
import com.example.common.security.JwtPrincipal;
import com.example.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public ReviewService(ReviewRepository reviewRepository, BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public ReviewResponse createReview(JwtPrincipal student, ReviewRequest request) {
        BookingEntity booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!student.userId().equals(booking.getStudentUserId())) {
            throw ApiException.forbidden("You can only review your own lessons.");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw ApiException.conflict("You can review a lesson once the tutor marks it as completed.");
        }
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw ApiException.conflict("You have already reviewed this lesson.");
        }
        ReviewEntity review = new ReviewEntity(booking.getId(), student.userId(), booking.getStudentName(),
                booking.getTutorUserId(), request.rating(), request.comment().trim());
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> reviewsForTutor(Long tutorUserId) {
        return reviewRepository.findByTutorUserIdOrderByCreatedAtDesc(tutorUserId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RatingSummaryResponse> ratingSummary() {
        return reviewRepository.summarizeRatings().stream()
                .map(view -> new RatingSummaryResponse(
                        view.getTutorUserId(),
                        Math.round(view.getAverageRating() * 10.0) / 10.0,
                        view.getReviewCount()))
                .toList();
    }
}

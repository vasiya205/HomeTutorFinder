package com.example.bookingservice.repository;

import com.example.bookingservice.domain.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    boolean existsByBookingId(Long bookingId);

    List<ReviewEntity> findByTutorUserIdOrderByCreatedAtDesc(Long tutorUserId);

    @Query("SELECT r.bookingId FROM ReviewEntity r WHERE r.studentUserId = :studentUserId")
    List<Long> findReviewedBookingIds(@Param("studentUserId") Long studentUserId);

    @Query("""
            SELECT r.tutorUserId AS tutorUserId, AVG(r.rating) AS averageRating, COUNT(r) AS reviewCount
            FROM ReviewEntity r
            GROUP BY r.tutorUserId
            """)
    List<RatingSummaryView> summarizeRatings();

    interface RatingSummaryView {
        Long getTutorUserId();

        Double getAverageRating();

        Long getReviewCount();
    }
}

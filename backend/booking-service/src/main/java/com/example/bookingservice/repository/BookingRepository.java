package com.example.bookingservice.repository;

import com.example.bookingservice.domain.BookingEntity;
import com.example.bookingservice.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    List<BookingEntity> findByStudentUserIdOrderByStartDateTimeDesc(Long studentUserId);

    List<BookingEntity> findByTutorUserIdOrderByStartDateTimeDesc(Long tutorUserId);

    List<BookingEntity> findAllByOrderByCreatedAtDesc();

    /** Two lessons overlap when one starts before the other ends (back-to-back lessons are fine). */
    @Query("""
            SELECT COUNT(b) FROM BookingEntity b
            WHERE b.tutorUserId = :tutorUserId
              AND b.status IN :statuses
              AND b.startDateTime < :end
              AND b.endDateTime > :start
            """)
    long countTutorOverlaps(@Param("tutorUserId") Long tutorUserId,
                            @Param("statuses") Collection<BookingStatus> statuses,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    @Query("""
            SELECT COUNT(b) FROM BookingEntity b
            WHERE b.studentUserId = :studentUserId
              AND b.status IN :statuses
              AND b.startDateTime < :end
              AND b.endDateTime > :start
            """)
    long countStudentOverlaps(@Param("studentUserId") Long studentUserId,
                              @Param("statuses") Collection<BookingStatus> statuses,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);
}

package com.example.tutorservice.repository;

import com.example.tutorservice.domain.TutorProfileEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TutorProfileRepository extends JpaRepository<TutorProfileEntity, Long> {
    Optional<TutorProfileEntity> findByUserId(Long userId);

    /** All text parameters are lower-case LIKE patterns ("%" matches everything). */
    @Query("""
            SELECT t FROM TutorProfileEntity t
            WHERE LOWER(t.subject) LIKE :subject
              AND LOWER(t.location) LIKE :location
              AND (LOWER(t.fullName) LIKE :keyword
                   OR LOWER(t.subject) LIKE :keyword
                   OR LOWER(t.qualification) LIKE :keyword
                   OR LOWER(t.bio) LIKE :keyword)
              AND t.hourlyRate >= :minRate
              AND t.hourlyRate <= :maxRate
            """)
    List<TutorProfileEntity> search(@Param("subject") String subject,
                                    @Param("location") String location,
                                    @Param("keyword") String keyword,
                                    @Param("minRate") BigDecimal minRate,
                                    @Param("maxRate") BigDecimal maxRate,
                                    Sort sort);

    @Query("SELECT DISTINCT t.subject FROM TutorProfileEntity t ORDER BY t.subject")
    List<String> findDistinctSubjects();
}

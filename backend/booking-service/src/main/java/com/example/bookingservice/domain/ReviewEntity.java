package com.example.bookingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class ReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Column(name = "student_name", nullable = false, length = 120)
    private String studentName;

    @Column(name = "tutor_user_id", nullable = false)
    private Long tutorUserId;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ReviewEntity() {}

    public ReviewEntity(Long bookingId, Long studentUserId, String studentName, Long tutorUserId,
                        Integer rating, String comment) {
        this.bookingId = bookingId;
        this.studentUserId = studentUserId;
        this.studentName = studentName;
        this.tutorUserId = tutorUserId;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public Long getStudentUserId() { return studentUserId; }
    public String getStudentName() { return studentName; }
    public Long getTutorUserId() { return tutorUserId; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

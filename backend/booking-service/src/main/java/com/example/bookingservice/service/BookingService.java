package com.example.bookingservice.service;

import com.example.bookingservice.api.BookingRequest;
import com.example.bookingservice.api.BookingResponse;
import com.example.bookingservice.api.BookingStatsResponse;
import com.example.bookingservice.client.TutorDirectoryClient;
import com.example.bookingservice.client.TutorDirectoryClient.TutorContact;
import com.example.bookingservice.domain.BookingEntity;
import com.example.bookingservice.domain.BookingStatus;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.ReviewRepository;
import com.example.common.security.JwtPrincipal;
import com.example.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {
    static final long MIN_DURATION_MINUTES = 30;
    static final long MAX_DURATION_MINUTES = 240;
    private static final Set<BookingStatus> BLOCKING_STATUSES =
            EnumSet.of(BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.COMPLETED);

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final TutorDirectoryClient tutorDirectoryClient;
    private final BookingNotifier notifier;

    public BookingService(BookingRepository bookingRepository, ReviewRepository reviewRepository,
                          TutorDirectoryClient tutorDirectoryClient, BookingNotifier notifier) {
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.tutorDirectoryClient = tutorDirectoryClient;
        this.notifier = notifier;
    }

    @Transactional
    public BookingResponse createBooking(JwtPrincipal student, BookingRequest request) {
        if (student.userId().equals(request.tutorUserId())) {
            throw ApiException.badRequest("You cannot book a lesson with yourself.");
        }
        validateTimes(request.startDateTime(), request.endDateTime(), LocalDateTime.now());

        TutorContact tutor = tutorDirectoryClient.getTutor(request.tutorUserId());

        if (bookingRepository.countTutorOverlaps(tutor.userId(), BLOCKING_STATUSES,
                request.startDateTime(), request.endDateTime()) > 0) {
            throw ApiException.conflict("The tutor is already booked at that time. Please pick another slot.");
        }
        if (bookingRepository.countStudentOverlaps(student.userId(), BLOCKING_STATUSES,
                request.startDateTime(), request.endDateTime()) > 0) {
            throw ApiException.conflict("You already have another lesson at that time.");
        }

        BookingEntity booking = new BookingEntity();
        booking.setStudentUserId(student.userId());
        booking.setStudentName(nonBlank(student.fullName(), "Student"));
        booking.setStudentEmail(nonBlank(student.email(), ""));
        booking.setTutorUserId(tutor.userId());
        booking.setTutorName(nonBlank(tutor.fullName(), "Tutor"));
        booking.setTutorEmail(nonBlank(tutor.email(), ""));
        booking.setSubject(request.subject().trim());
        booking.setStartDateTime(request.startDateTime());
        booking.setEndDateTime(request.endDateTime());
        booking.setNotes(request.notes() == null ? "" : request.notes().trim());
        // The price is always calculated on the server from the tutor's saved rate.
        booking.setHourlyRate(tutor.hourlyRate());
        booking.setTotalAmount(calculateTotalAmount(tutor.hourlyRate(), request.startDateTime(), request.endDateTime()));

        BookingEntity saved = bookingRepository.save(booking);
        notifier.bookingRequested(saved);
        return BookingResponse.from(saved, false);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listStudentBookings(Long studentUserId) {
        Set<Long> reviewed = new HashSet<>(reviewRepository.findReviewedBookingIds(studentUserId));
        return bookingRepository.findByStudentUserIdOrderByStartDateTimeDesc(studentUserId).stream()
                .map(booking -> BookingResponse.from(booking, reviewed.contains(booking.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listTutorBookings(Long tutorUserId) {
        return bookingRepository.findByTutorUserIdOrderByStartDateTimeDesc(tutorUserId).stream()
                .map(booking -> BookingResponse.from(booking, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(booking -> BookingResponse.from(booking, false))
                .toList();
    }

    @Transactional
    public BookingResponse updateStatus(JwtPrincipal actor, Long bookingId, BookingStatus next) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));

        boolean isStudent = actor.userId().equals(booking.getStudentUserId());
        boolean isTutor = actor.userId().equals(booking.getTutorUserId());
        validateTransition(isStudent, isTutor, booking, next);

        booking.setStatus(next);
        BookingEntity saved = bookingRepository.save(booking);
        switch (next) {
            case ACCEPTED -> notifier.bookingAccepted(saved);
            case REJECTED -> notifier.bookingRejected(saved);
            case COMPLETED -> notifier.bookingCompleted(saved);
            case CANCELLED -> notifier.bookingCancelled(saved, isTutor ? saved.getTutorName() : saved.getStudentName());
            default -> { }
        }
        return BookingResponse.from(saved, reviewRepository.existsByBookingId(saved.getId()));
    }

    @Transactional
    public BookingResponse cancelAsAdmin(JwtPrincipal admin, Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!booking.getStatus().isActive()) {
            throw ApiException.conflict("Only pending or accepted bookings can be cancelled.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        BookingEntity saved = bookingRepository.save(booking);
        notifier.bookingCancelled(saved, "the platform administrator");
        return BookingResponse.from(saved, false);
    }

    @Transactional(readOnly = true)
    public BookingStatsResponse stats() {
        List<BookingEntity> all = bookingRepository.findAll();
        BigDecimal revenue = all.stream()
                .filter(BookingEntity::isPaid)
                .map(BookingEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BookingStatsResponse(
                all.size(),
                count(all, BookingStatus.PENDING),
                count(all, BookingStatus.ACCEPTED),
                count(all, BookingStatus.COMPLETED),
                count(all, BookingStatus.REJECTED),
                count(all, BookingStatus.CANCELLED),
                all.stream().filter(BookingEntity::isPaid).count(),
                revenue);
    }

    /** The booking workflow rules, kept in one place (and unit tested). */
    static void validateTransition(boolean isStudent, boolean isTutor, BookingEntity booking, BookingStatus next) {
        if (!isStudent && !isTutor) {
            throw ApiException.forbidden("You can only update your own bookings.");
        }
        BookingStatus current = booking.getStatus();
        if (current == next) {
            throw ApiException.conflict("This booking is already " + current.name().toLowerCase() + ".");
        }
        switch (next) {
            case ACCEPTED, REJECTED -> {
                if (!isTutor) {
                    throw ApiException.forbidden("Only the tutor can accept or reject a request.");
                }
                if (current != BookingStatus.PENDING) {
                    throw ApiException.conflict("Only pending requests can be accepted or rejected.");
                }
            }
            case COMPLETED -> {
                if (!isTutor) {
                    throw ApiException.forbidden("Only the tutor can mark a lesson as completed.");
                }
                if (current != BookingStatus.ACCEPTED) {
                    throw ApiException.conflict("Only accepted lessons can be marked as completed.");
                }
                if (!booking.isPaid() && booking.getTotalAmount().signum() > 0) {
                    throw ApiException.conflict("The student has not paid for this lesson yet.");
                }
            }
            case CANCELLED -> {
                if (!current.isActive()) {
                    throw ApiException.conflict("Only pending or accepted bookings can be cancelled.");
                }
                if (booking.isPaid()) {
                    throw ApiException.conflict("Paid lessons cannot be cancelled online. Please contact support.");
                }
            }
            default -> throw ApiException.badRequest("Unsupported status change.");
        }
    }

    static void validateTimes(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (!start.isAfter(now)) {
            throw ApiException.badRequest("Please choose a start time in the future.");
        }
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("The end time must be after the start time.");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < MIN_DURATION_MINUTES || minutes > MAX_DURATION_MINUTES) {
            throw ApiException.badRequest("A lesson must be between 30 minutes and 4 hours long.");
        }
        if (start.isAfter(now.plusDays(90))) {
            throw ApiException.badRequest("Lessons can be booked up to 90 days in advance.");
        }
    }

    static BigDecimal calculateTotalAmount(BigDecimal hourlyRate, LocalDateTime start, LocalDateTime end) {
        long seconds = Duration.between(start, end).getSeconds();
        return hourlyRate.multiply(BigDecimal.valueOf(seconds))
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }

    private static long count(List<BookingEntity> bookings, BookingStatus status) {
        return bookings.stream().filter(booking -> booking.getStatus() == status).count();
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

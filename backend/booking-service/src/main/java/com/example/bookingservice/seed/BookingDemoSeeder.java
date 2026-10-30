package com.example.bookingservice.seed;

import com.example.bookingservice.domain.BookingEntity;
import com.example.bookingservice.domain.BookingStatus;
import com.example.bookingservice.domain.PaymentMode;
import com.example.bookingservice.domain.PaymentStatus;
import com.example.bookingservice.domain.ReviewEntity;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.ReviewRepository;
import com.example.common.demo.DemoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Seeds a realistic set of lessons so every dashboard has data on first run:
 * completed + reviewed lessons (ratings on tutor cards), an accepted lesson ready to pay,
 * and pending requests for tutors to accept.
 */
@Component
public class BookingDemoSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BookingDemoSeeder.class);

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final boolean enabled;

    public BookingDemoSeeder(BookingRepository bookingRepository, ReviewRepository reviewRepository,
                             @Value("${app.seed-demo-data:true}") boolean enabled) {
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || bookingRepository.count() > 0) {
            return;
        }
        DemoData.Account sneha = DemoData.STUDENT;
        DemoData.Account karthik = DemoData.STUDENT_TWO;
        DemoData.TutorProfile priya = DemoData.TUTORS.get(0);
        DemoData.TutorProfile arjun = DemoData.TUTORS.get(1);
        DemoData.TutorProfile ananya = DemoData.TUTORS.get(2);
        DemoData.TutorProfile rahul = DemoData.TUTORS.get(3);
        DemoData.TutorProfile fatima = DemoData.TUTORS.get(4);
        DemoData.TutorProfile vikram = DemoData.TUTORS.get(5);

        review(paid(booking(sneha, priya, -10, 17, 60, BookingStatus.COMPLETED, "Integration by parts and definite integrals")),
                5, "Priya explains calculus so clearly. I finally understand integration!");
        review(paid(booking(sneha, arjun, -6, 16, 90, BookingStatus.COMPLETED, "Rotational motion problems")),
                4, "Great examples and simulations. Would love a few more practice questions.");
        review(paid(booking(karthik, priya, -4, 10, 60, BookingStatus.COMPLETED, "Quadratic equations revision")),
                5, "Very patient and well prepared. Highly recommended for board exams.");
        review(paid(booking(karthik, vikram, -8, 18, 60, BookingStatus.COMPLETED, "Human physiology - circulatory system")),
                4, "Loved the mnemonics, made NEET topics much easier to remember.");
        review(paid(booking(karthik, ananya, -3, 11, 60, BookingStatus.COMPLETED, "IELTS speaking practice")),
                5, "Super friendly and my speaking confidence improved a lot.");

        booking(sneha, rahul, 2, 18, 60, BookingStatus.ACCEPTED, "Python basics - loops and functions");
        booking(sneha, ananya, 3, 11, 60, BookingStatus.PENDING, "Essay writing for school competition");
        booking(karthik, priya, 1, 19, 60, BookingStatus.PENDING, "Trigonometry identities");
        booking(karthik, fatima, 5, 15, 90, BookingStatus.PENDING, "Organic chemistry - reaction mechanisms");
        booking(karthik, arjun, 4, 19, 60, BookingStatus.CANCELLED, "Electrostatics doubt session");

        log.info("Seeded demo bookings and reviews");
    }

    private BookingEntity booking(DemoData.Account student, DemoData.TutorProfile tutor, int dayOffset, int hour,
                                  int minutes, BookingStatus status, String notes) {
        LocalDateTime start = LocalDate.now().plusDays(dayOffset).atTime(hour, 0);
        BookingEntity booking = new BookingEntity();
        booking.setStudentUserId(student.id());
        booking.setStudentName(student.fullName());
        booking.setStudentEmail(student.email());
        booking.setTutorUserId(tutor.userId());
        booking.setTutorName(tutor.fullName());
        booking.setTutorEmail(tutor.email());
        booking.setSubject(tutor.subject());
        booking.setStartDateTime(start);
        booking.setEndDateTime(start.plusMinutes(minutes));
        booking.setNotes(notes);
        booking.setHourlyRate(tutor.hourlyRate());
        booking.setTotalAmount(tutor.hourlyRate().multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    private BookingEntity paid(BookingEntity booking) {
        booking.setPaymentMode(PaymentMode.DEMO);
        booking.setPaymentOrderId("demo_order_seed_" + UUID.randomUUID().toString().substring(0, 8));
        booking.setPaymentId("demo_pay_seed_" + UUID.randomUUID().toString().substring(0, 8));
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setPaidAt(booking.getStartDateTime().minusDays(1));
        return bookingRepository.save(booking);
    }

    private void review(BookingEntity booking, int rating, String comment) {
        reviewRepository.save(new ReviewEntity(booking.getId(), booking.getStudentUserId(), booking.getStudentName(),
                booking.getTutorUserId(), rating, comment));
    }
}

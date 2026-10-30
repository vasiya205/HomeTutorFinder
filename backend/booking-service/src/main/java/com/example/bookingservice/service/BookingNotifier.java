package com.example.bookingservice.service;

import com.example.bookingservice.domain.BookingEntity;
import com.example.common.notification.NotificationClient;
import com.example.common.notification.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Builds the template data for every booking/payment email and hands it to the notification-service. */
@Component
public class BookingNotifier {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final NotificationClient notificationClient;
    private final String dashboardUrl;

    public BookingNotifier(NotificationClient notificationClient,
                           @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.notificationClient = notificationClient;
        this.dashboardUrl = (frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl)
                + "/dashboard";
    }

    public void bookingRequested(BookingEntity booking) {
        notificationClient.send(NotificationType.BOOKING_REQUESTED_TUTOR, booking.getTutorEmail(), booking.getTutorName(),
                details(booking));
        notificationClient.send(NotificationType.BOOKING_REQUESTED_STUDENT, booking.getStudentEmail(),
                booking.getStudentName(), details(booking));
    }

    public void bookingAccepted(BookingEntity booking) {
        notificationClient.send(NotificationType.BOOKING_ACCEPTED, booking.getStudentEmail(), booking.getStudentName(),
                details(booking));
    }

    public void bookingRejected(BookingEntity booking) {
        notificationClient.send(NotificationType.BOOKING_REJECTED, booking.getStudentEmail(), booking.getStudentName(),
                details(booking));
    }

    public void bookingCompleted(BookingEntity booking) {
        notificationClient.send(NotificationType.BOOKING_COMPLETED, booking.getStudentEmail(), booking.getStudentName(),
                details(booking));
    }

    /** Both participants are told, with who cancelled it. */
    public void bookingCancelled(BookingEntity booking, String cancelledBy) {
        Map<String, String> data = details(booking);
        data.put("cancelledBy", cancelledBy);
        notificationClient.send(NotificationType.BOOKING_CANCELLED, booking.getStudentEmail(), booking.getStudentName(), data);
        notificationClient.send(NotificationType.BOOKING_CANCELLED, booking.getTutorEmail(), booking.getTutorName(), data);
    }

    public void paymentSucceeded(BookingEntity booking) {
        Map<String, String> data = details(booking);
        data.put("paymentId", booking.getPaymentId());
        data.put("paymentMode", booking.getPaymentMode() == null ? "" : booking.getPaymentMode().name());
        data.put("paidAt", booking.getPaidAt() == null ? "" : booking.getPaidAt().format(DATE) + ", "
                + booking.getPaidAt().format(TIME));
        notificationClient.send(NotificationType.PAYMENT_RECEIPT, booking.getStudentEmail(), booking.getStudentName(), data);
        notificationClient.send(NotificationType.PAYMENT_RECEIVED, booking.getTutorEmail(), booking.getTutorName(), data);
    }

    private Map<String, String> details(BookingEntity booking) {
        Map<String, String> data = new HashMap<>();
        data.put("bookingId", String.valueOf(booking.getId()));
        data.put("subject", booking.getSubject());
        data.put("tutorName", booking.getTutorName());
        data.put("studentName", booking.getStudentName());
        data.put("date", booking.getStartDateTime().format(DATE));
        data.put("time", booking.getStartDateTime().format(TIME) + " - " + booking.getEndDateTime().format(TIME));
        data.put("duration", booking.durationMinutes() + " minutes");
        data.put("amount", "₹" + booking.getTotalAmount().toPlainString());
        data.put("notes", booking.getNotes() == null ? "" : booking.getNotes());
        data.put("dashboardUrl", dashboardUrl);
        return data;
    }
}

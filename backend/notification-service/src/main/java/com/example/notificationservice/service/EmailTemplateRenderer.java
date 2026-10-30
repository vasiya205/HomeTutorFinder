package com.example.notificationservice.service;

import com.example.common.notification.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds branded, mobile-friendly HTML emails (table layout + inline styles so they render
 * correctly in Gmail and Outlook). All user-supplied values are HTML-escaped.
 */
@Component
public class EmailTemplateRenderer {
    private static final String BRAND = "#4f46e5";

    private final String frontendUrl;

    public EmailTemplateRenderer(@Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    public RenderedEmail render(NotificationType type, String recipientName, Map<String, String> data) {
        String name = firstName(recipientName);
        String dashboard = data.getOrDefault("dashboardUrl", frontendUrl + "/dashboard");
        return switch (type) {
            case WELCOME -> "TUTOR".equals(data.get("role"))
                    ? email("Welcome to Home Tutor Finder, " + name + "!", "Welcome aboard, " + name + "!",
                    "Your tutor account is ready. Complete your profile so students can discover you, see your rates and send you lesson requests.",
                    List.of(), "Complete my tutor profile", frontendUrl + "/tutor-profile",
                    "Tip: tutors with a detailed bio and clear qualifications get the most requests.")
                    : email("Welcome to Home Tutor Finder, " + name + "!", "Welcome aboard, " + name + "!",
                    "Your student account is ready. Browse verified tutors by subject, location and price, then request a lesson in a couple of clicks.",
                    List.of(), "Find a tutor", frontendUrl + "/tutors",
                    "You will get an email whenever a tutor responds to your request.");
            case PASSWORD_RESET -> email("Reset your Home Tutor Finder password", "Reset your password",
                    "Hi " + name + ", we received a request to reset your password. Click the button below to choose a new one. "
                            + "This link can be used once and expires in " + esc(data.getOrDefault("validMinutes", "30")) + " minutes.",
                    List.of(), "Choose a new password", data.getOrDefault("resetUrl", frontendUrl + "/forgot-password"),
                    "If you did not request this, you can safely ignore this email - your password will not change.");
            case PASSWORD_CHANGED -> email("Your password was changed", "Password updated",
                    "Hi " + name + ", the password for your Home Tutor Finder account was just changed.",
                    List.of(), "Sign in", frontendUrl + "/login",
                    "If this was not you, reset your password immediately and contact the administrator.");
            case BOOKING_REQUESTED_TUTOR -> email("New lesson request from " + data.get("studentName"),
                    "You have a new lesson request",
                    "Hi " + name + ", <strong>" + esc(data.get("studentName")) + "</strong> would like to book a lesson with you.",
                    lessonRows(data, true), "Review the request", dashboard,
                    "Please accept or reject the request soon so the student can plan ahead.");
            case BOOKING_REQUESTED_STUDENT -> email("Lesson request sent to " + data.get("tutorName"),
                    "Your request has been sent",
                    "Hi " + name + ", your lesson request was sent to <strong>" + esc(data.get("tutorName"))
                            + "</strong>. We will email you as soon as they respond.",
                    lessonRows(data, false), "View my bookings", dashboard, null);
            case BOOKING_ACCEPTED -> email(data.get("tutorName") + " accepted your lesson - complete payment",
                    "Your lesson is confirmed!",
                    "Great news, " + name + "! <strong>" + esc(data.get("tutorName"))
                            + "</strong> accepted your request. Complete the payment to lock in your slot.",
                    lessonRows(data, false), "Pay " + data.getOrDefault("amount", "") + " now", dashboard, null);
            case BOOKING_REJECTED -> email(data.get("tutorName") + " could not take your lesson",
                    "Request not accepted",
                    "Hi " + name + ", unfortunately <strong>" + esc(data.get("tutorName"))
                            + "</strong> is not available for this lesson. No payment was taken.",
                    lessonRows(data, false), "Find another tutor", frontendUrl + "/tutors", null);
            case BOOKING_CANCELLED -> email("Lesson cancelled: " + data.get("subject") + " on " + data.get("date"),
                    "Lesson cancelled",
                    "Hi " + name + ", the lesson below was cancelled by " + esc(data.getOrDefault("cancelledBy", "a participant")) + ".",
                    lessonRows(data, true), "Open dashboard", dashboard, null);
            case BOOKING_COMPLETED -> email("How was your " + data.get("subject") + " lesson?",
                    "Lesson completed",
                    "Hi " + name + ", <strong>" + esc(data.get("tutorName"))
                            + "</strong> marked your lesson as completed. Your feedback helps other students choose the right tutor.",
                    lessonRows(data, false), "Leave a review", dashboard, null);
            case PAYMENT_RECEIPT -> email("Payment receipt - " + data.get("amount") + " for " + data.get("subject"),
                    "Payment successful",
                    "Hi " + name + ", thank you! Your payment was received and your lesson is fully confirmed.",
                    receiptRows(data), "View my bookings", dashboard, paymentNote(data));
            case PAYMENT_RECEIVED -> email("Payment received from " + data.get("studentName"),
                    "You have been paid for a lesson",
                    "Hi " + name + ", <strong>" + esc(data.get("studentName")) + "</strong> has paid for the lesson below.",
                    receiptRows(data), "Open dashboard", dashboard, paymentNote(data));
            case TEST -> email("Test email from Home Tutor Finder", "Email delivery works!",
                    "Hi " + name + ", this is a test email sent from the Home Tutor Finder admin dashboard. "
                            + "If you can read this, SMTP is configured correctly.",
                    List.<String[]>of(new String[]{"Sent by", esc(data.getOrDefault("sentBy", "Administrator"))}),
                    "Open Home Tutor Finder", frontendUrl, null);
        };
    }

    private static List<String[]> lessonRows(Map<String, String> data, boolean includeNotes) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Subject", esc(data.get("subject"))});
        rows.add(new String[]{"Tutor", esc(data.get("tutorName"))});
        rows.add(new String[]{"Student", esc(data.get("studentName"))});
        rows.add(new String[]{"Date", esc(data.get("date"))});
        rows.add(new String[]{"Time", esc(data.get("time")) + " (" + esc(data.get("duration")) + ")"});
        rows.add(new String[]{"Lesson fee", esc(data.get("amount"))});
        if (includeNotes && data.get("notes") != null && !data.get("notes").isBlank()) {
            rows.add(new String[]{"Notes", esc(data.get("notes"))});
        }
        rows.add(new String[]{"Booking ID", "#" + esc(data.get("bookingId"))});
        return rows;
    }

    private static List<String[]> receiptRows(Map<String, String> data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Amount paid", "<strong>" + esc(data.get("amount")) + "</strong>"});
        rows.add(new String[]{"Payment ID", esc(data.get("paymentId"))});
        rows.add(new String[]{"Paid on", esc(data.get("paidAt"))});
        rows.add(new String[]{"Lesson", esc(data.get("subject")) + " with " + esc(data.get("tutorName"))});
        rows.add(new String[]{"Student", esc(data.get("studentName"))});
        rows.add(new String[]{"Date & time", esc(data.get("date")) + ", " + esc(data.get("time"))});
        rows.add(new String[]{"Booking ID", "#" + esc(data.get("bookingId"))});
        return rows;
    }

    private static String paymentNote(Map<String, String> data) {
        return "DEMO".equals(data.get("paymentMode"))
                ? "This was a demo payment - no real money was charged."
                : "Processed securely by Razorpay (test mode) - no real money was charged.";
    }

    private RenderedEmail email(String subject, String heading, String introHtml, List<String[]> rows,
                                String buttonText, String buttonUrl, String footnote) {
        StringBuilder table = new StringBuilder();
        if (!rows.isEmpty()) {
            table.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ")
                    .append("style=\"margin:24px 0;border:1px solid #e5e7eb;border-radius:12px;border-collapse:separate;overflow:hidden\">");
            for (int i = 0; i < rows.size(); i++) {
                String background = i % 2 == 0 ? "#f9fafb" : "#ffffff";
                table.append("<tr style=\"background:").append(background).append("\">")
                        .append("<td style=\"padding:10px 16px;color:#6b7280;font-size:13px;width:38%\">")
                        .append(rows.get(i)[0]).append("</td>")
                        .append("<td style=\"padding:10px 16px;color:#111827;font-size:14px\">")
                        .append(rows.get(i)[1]).append("</td></tr>");
            }
            table.append("</table>");
        }
        String button = "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:8px 0 4px\"><tr>"
                + "<td style=\"border-radius:10px;background:" + BRAND + "\">"
                + "<a href=\"" + esc(buttonUrl) + "\" style=\"display:inline-block;padding:13px 26px;color:#ffffff;"
                + "font-weight:600;font-size:15px;text-decoration:none;border-radius:10px\">" + esc(buttonText) + "</a>"
                + "</td></tr></table>";
        String note = footnote == null ? "" :
                "<p style=\"margin:20px 0 0;color:#6b7280;font-size:13px;line-height:1.6\">" + esc(footnote) + "</p>";

        String html = """
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>%s</title></head>
                <body style="margin:0;padding:0;background:#f3f4f6;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:32px 12px">
                <tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:580px">
                  <tr><td style="background:linear-gradient(135deg,#4f46e5,#7c3aed);background-color:#4f46e5;border-radius:16px 16px 0 0;padding:26px 32px">
                    <span style="font-size:20px;font-weight:700;color:#ffffff;letter-spacing:-0.2px">&#127891; Home Tutor Finder</span>
                  </td></tr>
                  <tr><td style="background:#ffffff;padding:32px;border-radius:0 0 16px 16px;box-shadow:0 4px 18px rgba(17,24,39,0.06)">
                    <h1 style="margin:0 0 12px;font-size:22px;color:#111827">%s</h1>
                    <p style="margin:0;color:#374151;font-size:15px;line-height:1.65">%s</p>
                    %s
                    %s
                    %s
                  </td></tr>
                  <tr><td style="padding:20px 32px;text-align:center;color:#9ca3af;font-size:12px;line-height:1.6">
                    You received this email because you have an account on Home Tutor Finder.<br>
                    &copy; Home Tutor Finder &middot; Learn from the best tutors near you
                  </td></tr>
                </table>
                </td></tr></table>
                </body></html>
                """.formatted(esc(subject), esc(heading), introHtml, table, button, note);
        return new RenderedEmail(subject, html);
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "there";
        }
        return esc(fullName.trim().split("\\s+")[0]);
    }

    static String esc(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}

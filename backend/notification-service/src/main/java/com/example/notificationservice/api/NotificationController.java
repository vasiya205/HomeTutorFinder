package com.example.notificationservice.api;

import com.example.common.notification.NotificationRequest;
import com.example.common.notification.NotificationType;
import com.example.common.security.JwtPrincipal;
import com.example.common.web.ApiException;
import com.example.notificationservice.domain.EmailNotificationEntity;
import com.example.notificationservice.domain.EmailStatus;
import com.example.notificationservice.service.EmailDispatcher;
import com.example.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final EmailDispatcher dispatcher;

    public NotificationController(NotificationService notificationService, EmailDispatcher dispatcher) {
        this.notificationService = notificationService;
        this.dispatcher = dispatcher;
    }

    /** Internal: other services queue an email here (requires a service JWT). */
    @PostMapping("/email")
    public ResponseEntity<EmailNotificationResponse> queue(@RequestBody NotificationRequest request) {
        EmailNotificationEntity saved = notificationService.queue(request);
        dispatcher.dispatch(saved.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(EmailNotificationResponse.summary(saved));
    }

    /** The signed-in user's inbox: every email the platform sent them (with the rendered HTML). */
    @GetMapping("/my")
    public List<EmailNotificationResponse> myEmails(@AuthenticationPrincipal JwtPrincipal principal) {
        return notificationService.forRecipient(principal.email()).stream()
                .map(EmailNotificationResponse::full)
                .toList();
    }

    @GetMapping("/{id}")
    public EmailNotificationResponse one(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id) {
        EmailNotificationEntity email = notificationService.get(id);
        if (!principal.isAdmin() && !email.getRecipientEmail().equalsIgnoreCase(String.valueOf(principal.email()))) {
            throw ApiException.forbidden("You can only open your own emails.");
        }
        return EmailNotificationResponse.full(email);
    }

    @GetMapping("/admin")
    public List<EmailNotificationResponse> allEmails() {
        return notificationService.latest().stream().map(EmailNotificationResponse::summary).toList();
    }

    @GetMapping("/admin/status")
    public MailStatusResponse status() {
        return new MailStatusResponse(
                dispatcher.isSmtpConfigured(),
                dispatcher.fromAddress(),
                notificationService.total(),
                notificationService.count(EmailStatus.SENT),
                notificationService.count(EmailStatus.LOGGED),
                notificationService.count(EmailStatus.FAILED));
    }

    /** Lets the admin prove email delivery live (e.g. during a demo). */
    @PostMapping("/admin/test")
    public ResponseEntity<EmailNotificationResponse> sendTest(@AuthenticationPrincipal JwtPrincipal admin,
                                                              @Valid @RequestBody TestEmailRequest request) {
        EmailNotificationEntity saved = notificationService.queue(new NotificationRequest(
                NotificationType.TEST, request.to().trim(), null,
                Map.of("sentBy", admin.fullName() == null ? "Administrator" : admin.fullName())));
        dispatcher.dispatch(saved.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(EmailNotificationResponse.summary(saved));
    }
}

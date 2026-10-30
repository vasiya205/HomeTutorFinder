package com.example.notificationservice.service;

import com.example.common.notification.NotificationRequest;
import com.example.common.web.ApiException;
import com.example.notificationservice.domain.EmailNotificationEntity;
import com.example.notificationservice.domain.EmailStatus;
import com.example.notificationservice.repository.EmailNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    private final EmailNotificationRepository repository;
    private final EmailTemplateRenderer renderer;

    public NotificationService(EmailNotificationRepository repository, EmailTemplateRenderer renderer) {
        this.repository = repository;
        this.renderer = renderer;
    }

    /** Renders and stores the email; the caller then hands the id to {@link EmailDispatcher}. */
    @Transactional
    public EmailNotificationEntity queue(NotificationRequest request) {
        if (request.type() == null) {
            throw ApiException.badRequest("Notification type is required");
        }
        if (request.recipientEmail() == null || !request.recipientEmail().contains("@")) {
            throw ApiException.badRequest("A valid recipient email is required");
        }
        RenderedEmail rendered = renderer.render(request.type(), request.recipientName(), request.data());
        String subject = rendered.subject().length() > 200 ? rendered.subject().substring(0, 200) : rendered.subject();
        return repository.save(new EmailNotificationEntity(request.type().name(), request.recipientEmail().trim(),
                request.recipientName(), subject, rendered.html()));
    }

    @Transactional(readOnly = true)
    public List<EmailNotificationEntity> latest() {
        return repository.findTop200ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<EmailNotificationEntity> forRecipient(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return repository.findTop50ByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public EmailNotificationEntity get(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Email not found"));
    }

    @Transactional(readOnly = true)
    public long count(EmailStatus status) {
        return repository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long total() {
        return repository.count();
    }
}

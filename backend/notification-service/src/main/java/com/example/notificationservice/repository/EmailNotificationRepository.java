package com.example.notificationservice.repository;

import com.example.notificationservice.domain.EmailNotificationEntity;
import com.example.notificationservice.domain.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailNotificationRepository extends JpaRepository<EmailNotificationEntity, Long> {
    List<EmailNotificationEntity> findTop200ByOrderByCreatedAtDesc();

    List<EmailNotificationEntity> findTop50ByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(String recipientEmail);

    long countByStatus(EmailStatus status);
}

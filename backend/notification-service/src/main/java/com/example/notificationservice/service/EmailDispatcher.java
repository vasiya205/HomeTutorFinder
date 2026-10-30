package com.example.notificationservice.service;

import com.example.common.demo.DemoData;
import com.example.notificationservice.domain.EmailNotificationEntity;
import com.example.notificationservice.domain.EmailStatus;
import com.example.notificationservice.repository.EmailNotificationRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Sends a stored email over SMTP in the background and records the outcome. */
@Component
public class EmailDispatcher {
    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    private final EmailNotificationRepository repository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String fromAddress;
    private final String fromName;

    public EmailDispatcher(EmailNotificationRepository repository,
                           ObjectProvider<JavaMailSender> mailSenderProvider,
                           @Value("${spring.mail.username:}") String smtpUsername,
                           @Value("${spring.mail.password:}") String smtpPassword,
                           @Value("${mail.from-address:}") String fromAddress,
                           @Value("${mail.from-name:Home Tutor Finder}") String fromName) {
        this.repository = repository;
        this.mailSenderProvider = mailSenderProvider;
        this.smtpUsername = smtpUsername == null ? "" : smtpUsername.trim();
        this.smtpPassword = smtpPassword == null ? "" : smtpPassword.trim();
        this.fromAddress = fromAddress == null || fromAddress.isBlank() ? this.smtpUsername : fromAddress.trim();
        this.fromName = fromName;
        if (isSmtpConfigured()) {
            log.info("SMTP is configured - emails will be delivered from {}", this.fromAddress);
        } else {
            log.warn("SMTP is not configured (MAIL_USERNAME / MAIL_PASSWORD empty) - emails are saved to the "
                    + "database and shown in the app, but not delivered.");
        }
    }

    public boolean isSmtpConfigured() {
        return !smtpUsername.isEmpty() && !smtpPassword.isEmpty() && mailSenderProvider.getIfAvailable() != null;
    }

    public String fromAddress() {
        return fromAddress;
    }

    @Async
    public void dispatch(Long notificationId) {
        EmailNotificationEntity email = repository.findById(notificationId).orElse(null);
        if (email == null) {
            return;
        }
        if (DemoData.isDemoEmail(email.getRecipientEmail())) {
            finish(email, EmailStatus.LOGGED, "Demo address - stored only, not delivered.");
            return;
        }
        if (!isSmtpConfigured()) {
            finish(email, EmailStatus.LOGGED, "SMTP not configured - set MAIL_USERNAME and MAIL_PASSWORD to deliver emails.");
            return;
        }
        try {
            JavaMailSender mailSender = mailSenderProvider.getObject();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email.getRecipientEmail());
            helper.setSubject(email.getSubject());
            helper.setText(toPlainText(email.getHtmlBody()), email.getHtmlBody());
            mailSender.send(message);
            email.setSentAt(LocalDateTime.now());
            finish(email, EmailStatus.SENT, null);
            log.info("Email '{}' sent to {}", email.getSubject(), email.getRecipientEmail());
        } catch (Exception exception) {
            String reason = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            finish(email, EmailStatus.FAILED, reason.length() > 480 ? reason.substring(0, 480) : reason);
            log.warn("Email '{}' to {} failed: {}", email.getSubject(), email.getRecipientEmail(), reason);
        }
    }

    private void finish(EmailNotificationEntity email, EmailStatus status, String note) {
        email.setStatus(status);
        email.setErrorMessage(note);
        repository.save(email);
        if (status == EmailStatus.LOGGED) {
            log.info("[email:{}] to={} subject=\"{}\" ({})", email.getType(), email.getRecipientEmail(),
                    email.getSubject(), note);
        }
    }

    static String toPlainText(String html) {
        return html.replaceAll("(?is)<(style|title)[^>]*>.*?</\\1>", "")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|tr|h1|table)>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("&#\\d+;", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\s*\\n\\s*", "\n")
                .trim();
    }
}

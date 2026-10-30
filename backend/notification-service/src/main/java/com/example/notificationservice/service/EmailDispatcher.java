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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

/** Sends a stored email via Brevo HTTP API (works on Render) or SMTP (local), and records the outcome. */
@Component
public class EmailDispatcher {
    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final EmailNotificationRepository repository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String brevoApiKey;
    private final String fromAddress;
    private final String fromName;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public EmailDispatcher(EmailNotificationRepository repository,
                           ObjectProvider<JavaMailSender> mailSenderProvider,
                           @Value("${spring.mail.username:}") String smtpUsername,
                           @Value("${spring.mail.password:}") String smtpPassword,
                           @Value("${BREVO_API_KEY:}") String brevoApiKey,
                           @Value("${MAIL_FROM:}") String mailFrom,
                           @Value("${mail.from-address:}") String fromAddress,
                           @Value("${mail.from-name:Home Tutor Finder}") String fromName) {
        this.repository = repository;
        this.mailSenderProvider = mailSenderProvider;
        this.smtpUsername = smtpUsername == null ? "" : smtpUsername.trim();
        this.smtpPassword = smtpPassword == null ? "" : smtpPassword.trim();
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
        this.fromAddress = firstNonBlank(fromAddress, mailFrom, this.smtpUsername);
        this.fromName = fromName;
        if (isBrevoConfigured()) {
            log.info("Brevo API is configured - emails will be delivered from {}", this.fromAddress);
        } else if (isSmtpOnlyConfigured()) {
            log.info("SMTP is configured - emails will be delivered from {}", this.fromAddress);
        } else {
            log.warn("No mail provider configured (BREVO_API_KEY or MAIL_USERNAME / MAIL_PASSWORD) - "
                    + "emails are saved to the database but not delivered.");
        }
    }

    /** Kept with the old name so other classes still compile: true if ANY mail provider is ready. */
    public boolean isSmtpConfigured() {
        return isBrevoConfigured() || isSmtpOnlyConfigured();
    }

    private boolean isBrevoConfigured() {
        return !brevoApiKey.isEmpty() && !fromAddress.isEmpty();
    }

    private boolean isSmtpOnlyConfigured() {
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
            finish(email, EmailStatus.LOGGED, "No mail provider configured - set BREVO_API_KEY (or MAIL_USERNAME and MAIL_PASSWORD).");
            return;
        }
        try {
            if (isBrevoConfigured()) {
                sendViaBrevo(email);
            } else {
                sendViaSmtp(email);
            }
            email.setSentAt(LocalDateTime.now());
            finish(email, EmailStatus.SENT, null);
            log.info("Email '{}' sent to {}", email.getSubject(), email.getRecipientEmail());
        } catch (Exception exception) {
            String reason = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            finish(email, EmailStatus.FAILED, reason.length() > 480 ? reason.substring(0, 480) : reason);
            log.warn("Email '{}' to {} failed: {}", email.getSubject(), email.getRecipientEmail(), reason);
        }
    }

    private void sendViaBrevo(EmailNotificationEntity email) throws Exception {
        String html = email.getHtmlBody() == null ? "" : email.getHtmlBody();
        String json = "{"
                + "\"sender\":{\"name\":" + json(fromName) + ",\"email\":" + json(fromAddress) + "},"
                + "\"to\":[{\"email\":" + json(email.getRecipientEmail()) + "}],"
                + "\"subject\":" + json(email.getSubject()) + ","
                + "\"htmlContent\":" + json(html) + ","
                + "\"textContent\":" + json(toPlainText(html))
                + "}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(BREVO_URL))
                .timeout(Duration.ofSeconds(20))
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                .header("accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Brevo error " + response.statusCode() + ": " + response.body());
        }
    }

    private void sendViaSmtp(EmailNotificationEntity email) throws Exception {
        JavaMailSender mailSender = mailSenderProvider.getObject();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress, fromName);
        helper.setTo(email.getRecipientEmail());
        helper.setSubject(email.getSubject());
        helper.setText(toPlainText(email.getHtmlBody()), email.getHtmlBody());
        mailSender.send(message);
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

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    /** Turns text into a safe JSON string value. */
    static String json(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    static String toPlainText(String html) {
        if (html == null) {
            return "";
        }
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
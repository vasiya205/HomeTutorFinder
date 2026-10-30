package com.example.common.notification;

import com.example.common.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fire-and-forget client for the notification-service. Emails are sent in the background,
 * so a slow or stopped mail server never blocks sign-up, booking or payment.
 */
@Component
public class NotificationClient {
    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;
    private final JwtService jwtService;
    private final String serviceName;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public NotificationClient(RestClient.Builder restClientBuilder,
                              JwtService jwtService,
                              @Value("${notification-service.url:http://localhost:8084}") String baseUrl,
                              @Value("${spring.application.name:service}") String serviceName) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.jwtService = jwtService;
        this.serviceName = serviceName;
    }

    public void send(NotificationType type, String recipientEmail, String recipientName, Map<String, String> data) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return;
        }
        NotificationRequest request = new NotificationRequest(type, recipientEmail, recipientName, data);
        executor.execute(() -> {
            try {
                restClient.post()
                        .uri("/api/notifications/email")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateServiceToken(serviceName))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception exception) {
                log.warn("Could not queue {} email for {} (is notification-service running?): {}",
                        type, recipientEmail, exception.getMessage());
            }
        });
    }
}

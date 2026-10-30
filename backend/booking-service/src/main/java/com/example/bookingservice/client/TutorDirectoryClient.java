package com.example.bookingservice.client;

import com.example.common.security.JwtService;
import com.example.common.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/** Looks up a tutor's current rate, name and email from tutor-service (service-to-service call). */
@Component
public class TutorDirectoryClient {
    private final RestClient restClient;
    private final JwtService jwtService;

    public TutorDirectoryClient(RestClient.Builder builder,
                                JwtService jwtService,
                                @Value("${tutor-service.url:http://localhost:8082}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.jwtService = jwtService;
    }

    public TutorContact getTutor(Long tutorUserId) {
        try {
            TutorContact tutor = restClient.get()
                    .uri("/api/tutors/internal/user/{userId}", tutorUserId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateServiceToken("booking-service"))
                    .retrieve()
                    .body(TutorContact.class);
            if (tutor == null) {
                throw ApiException.unavailable("Tutor service returned an empty response. Please try again.");
            }
            return tutor;
        } catch (HttpClientErrorException.NotFound exception) {
            throw ApiException.badRequest("This tutor has not published a profile yet.");
        } catch (RestClientException exception) {
            throw ApiException.unavailable("Tutor service is not reachable right now, so the booking was not created.");
        }
    }

    public record TutorContact(Long userId, String fullName, String email, String subject, BigDecimal hourlyRate) {}
}

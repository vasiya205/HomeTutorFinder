package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Routes /api/auth/** -> auth-service, /api/tutors/** -> tutor-service,
 * /api/bookings|reviews|admin/bookings/** -> booking-service, /api/notifications/** -> notification-service.
 * Each service validates the JWT itself, so the gateway stays a thin, fast router.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

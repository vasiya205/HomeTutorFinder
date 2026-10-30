package com.example.tutorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.example.tutorservice", "com.example.common"})
@EnableDiscoveryClient
public class TutorServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TutorServiceApplication.class, args);
    }
}

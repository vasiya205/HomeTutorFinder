package com.example.tutorservice.api;

import com.example.tutorservice.service.TutorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Called by booking-service with a service JWT (see SecurityConfig). */
@RestController
@RequestMapping("/api/tutors/internal")
public class InternalTutorController {
    private final TutorService tutorService;

    public InternalTutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping("/user/{userId}")
    public TutorContactResponse contact(@PathVariable Long userId) {
        return tutorService.getContact(userId);
    }
}

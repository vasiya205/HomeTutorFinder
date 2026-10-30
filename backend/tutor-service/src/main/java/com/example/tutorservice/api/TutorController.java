package com.example.tutorservice.api;

import com.example.common.security.JwtPrincipal;
import com.example.tutorservice.service.TutorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/tutors")
public class TutorController {
    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    /**
     * Public search. Example: /api/tutors/search?subject=math&location=hyderabad&maxRate=900&sort=rate_asc
     * sort = newest | rate_asc | rate_desc | experience
     */
    @GetMapping("/search")
    public List<TutorProfileResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String subject,
                                             @RequestParam(required = false) String location,
                                             @RequestParam(required = false) BigDecimal minRate,
                                             @RequestParam(required = false) BigDecimal maxRate,
                                             @RequestParam(required = false, defaultValue = "experience") String sort) {
        return tutorService.search(q, subject, location, minRate, maxRate, sort);
    }

    @GetMapping("/subjects")
    public List<String> subjects() {
        return tutorService.subjects();
    }

    @GetMapping("/{id}")
    public TutorProfileResponse getById(@PathVariable Long id) {
        return tutorService.getProfile(id);
    }

    @GetMapping("/user/{userId}")
    public TutorProfileResponse getByUserId(@PathVariable Long userId) {
        return tutorService.getProfileByUserId(userId);
    }

    /** The signed-in tutor's own profile (204 No Content when they have not created one yet). */
    @GetMapping("/me")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<TutorProfileResponse> myProfile(@AuthenticationPrincipal JwtPrincipal principal) {
        return tutorService.findMyProfile(principal.userId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('TUTOR')")
    public TutorProfileResponse saveMyProfile(@AuthenticationPrincipal JwtPrincipal principal,
                                              @Valid @RequestBody TutorProfileRequest request) {
        return tutorService.saveProfile(principal, request);
    }

    /** Kept for backwards compatibility with the original API (same as PUT /api/tutors/me). */
    @PostMapping("/profile")
    @PreAuthorize("hasRole('TUTOR')")
    public TutorProfileResponse upsertProfile(@AuthenticationPrincipal JwtPrincipal principal,
                                              @Valid @RequestBody TutorProfileRequest request) {
        return tutorService.saveProfile(principal, request);
    }
}

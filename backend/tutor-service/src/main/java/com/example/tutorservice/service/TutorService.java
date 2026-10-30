package com.example.tutorservice.service;

import com.example.common.security.JwtPrincipal;
import com.example.common.web.ApiException;
import com.example.tutorservice.api.TutorContactResponse;
import com.example.tutorservice.api.TutorProfileRequest;
import com.example.tutorservice.api.TutorProfileResponse;
import com.example.tutorservice.domain.TeachingMode;
import com.example.tutorservice.domain.TutorProfileEntity;
import com.example.tutorservice.repository.TutorProfileRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class TutorService {
    private static final BigDecimal MAX_RATE = new BigDecimal("1000000");

    private final TutorProfileRepository repository;

    public TutorService(TutorProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TutorProfileResponse saveProfile(JwtPrincipal tutor, TutorProfileRequest request) {
        TutorProfileEntity profile = repository.findByUserId(tutor.userId()).orElseGet(TutorProfileEntity::new);
        profile.setUserId(tutor.userId());
        profile.setFullName(tutor.fullName() == null || tutor.fullName().isBlank() ? "Tutor" : tutor.fullName());
        profile.setEmail(tutor.email() == null ? "" : tutor.email());
        profile.setQualification(request.qualification().trim());
        profile.setSubject(capitalize(request.subject().trim()));
        profile.setLocation(capitalize(request.location().trim()));
        profile.setExperienceYears(request.experienceYears());
        profile.setBio(request.bio().trim());
        profile.setHourlyRate(request.hourlyRate());
        profile.setTeachingMode(request.teachingMode() == null ? TeachingMode.BOTH : request.teachingMode());
        return TutorProfileResponse.from(repository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<TutorProfileResponse> search(String keyword, String subject, String location,
                                             BigDecimal minRate, BigDecimal maxRate, String sort) {
        return repository.search(
                        likePattern(subject),
                        likePattern(location),
                        likePattern(keyword),
                        minRate == null ? BigDecimal.ZERO : minRate,
                        maxRate == null ? MAX_RATE : maxRate,
                        toSort(sort))
                .stream()
                .map(TutorProfileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> subjects() {
        return repository.findDistinctSubjects();
    }

    @Transactional(readOnly = true)
    public TutorProfileResponse getProfile(Long id) {
        return repository.findById(id)
                .map(TutorProfileResponse::from)
                .orElseThrow(() -> ApiException.notFound("Tutor profile not found"));
    }

    @Transactional(readOnly = true)
    public TutorProfileResponse getProfileByUserId(Long userId) {
        return repository.findByUserId(userId)
                .map(TutorProfileResponse::from)
                .orElseThrow(() -> ApiException.notFound("Tutor profile not found"));
    }

    @Transactional(readOnly = true)
    public Optional<TutorProfileResponse> findMyProfile(Long userId) {
        return repository.findByUserId(userId).map(TutorProfileResponse::from);
    }

    @Transactional(readOnly = true)
    public TutorContactResponse getContact(Long userId) {
        return repository.findByUserId(userId)
                .map(TutorContactResponse::from)
                .orElseThrow(() -> ApiException.notFound("Tutor profile not found"));
    }

    static String likePattern(String value) {
        if (value == null || value.isBlank()) {
            return "%";
        }
        String escaped = value.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "");
        return "%" + escaped + "%";
    }

    static Sort toSort(String sort) {
        String key = sort == null ? "" : sort.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "rate_asc" -> Sort.by(Sort.Direction.ASC, "hourlyRate");
            case "rate_desc" -> Sort.by(Sort.Direction.DESC, "hourlyRate");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "experienceYears").and(Sort.by("fullName"));
        };
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}

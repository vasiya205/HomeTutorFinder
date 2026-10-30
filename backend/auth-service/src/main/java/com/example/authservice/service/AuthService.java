package com.example.authservice.service;

import com.example.authservice.api.AdminUserResponse;
import com.example.authservice.api.AuthResponse;
import com.example.authservice.api.LoginRequest;
import com.example.authservice.api.RegisterRequest;
import com.example.authservice.api.UserSummary;
import com.example.authservice.domain.Role;
import com.example.authservice.domain.UserEntity;
import com.example.authservice.repository.UserRepository;
import com.example.common.notification.NotificationClient;
import com.example.common.notification.NotificationType;
import com.example.common.security.JwtService;
import com.example.common.web.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationClient notificationClient;
    /** Compared against when the email is unknown, so response time does not reveal which emails exist. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, NotificationClient notificationClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificationClient = notificationClient;
        this.dummyHash = passwordEncoder.encode("timing-safe-placeholder");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        Role role = request.role() == null ? Role.STUDENT : request.role();
        if (role == Role.ADMIN) {
            throw ApiException.forbidden("Admin accounts cannot be created through public sign-up.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists. Please sign in instead.");
        }

        UserEntity user = new UserEntity(request.fullName().trim(), email, passwordEncoder.encode(request.password()), role);
        user.setLastLoginAt(LocalDateTime.now());
        UserEntity saved = userRepository.save(user);

        notificationClient.send(NotificationType.WELCOME, saved.getEmail(), saved.getFullName(),
                Map.of("role", saved.getRole().name()));
        return toAuthResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
        if (!user.isEnabled()) {
            throw ApiException.forbidden("This account has been disabled. Please contact the administrator.");
        }
        user.setLastLoginAt(LocalDateTime.now());
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserSummary currentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserSummary::from)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(AdminUserResponse::from).toList();
    }

    @Transactional
    public AdminUserResponse setAccountEnabled(Long adminUserId, Long targetUserId, boolean enabled) {
        UserEntity target = userRepository.findById(targetUserId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (!enabled && adminUserId.equals(targetUserId)) {
            throw ApiException.badRequest("You cannot disable your own admin account.");
        }
        if (!enabled && target.getRole() == Role.ADMIN && target.isEnabled()
                && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw ApiException.conflict("The last enabled admin account cannot be disabled.");
        }
        target.setEnabled(enabled);
        return AdminUserResponse.from(userRepository.save(target));
    }

    private AuthResponse toAuthResponse(UserEntity user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
        return new AuthResponse(token, "Bearer", jwtService.userTokenTtlSeconds(), UserSummary.from(user));
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}

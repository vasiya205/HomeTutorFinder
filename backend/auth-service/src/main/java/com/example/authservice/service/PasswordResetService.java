package com.example.authservice.service;

import com.example.authservice.domain.PasswordResetTokenEntity;
import com.example.authservice.domain.UserEntity;
import com.example.authservice.repository.PasswordResetTokenRepository;
import com.example.authservice.repository.UserRepository;
import com.example.common.notification.NotificationClient;
import com.example.common.notification.NotificationType;
import com.example.common.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Service
public class PasswordResetService {
    private static final int TOKEN_VALID_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationClient notificationClient;
    private final String frontendUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                NotificationClient notificationClient,
                                @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationClient = notificationClient;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    @Transactional
    public void requestReset(String rawEmail) {
        UserEntity user = userRepository.findByEmailIgnoreCase(AuthService.normalizeEmail(rawEmail)).orElse(null);
        if (user == null || !user.isEnabled()) {
            return; // Same response either way - do not reveal whether the email is registered.
        }
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.invalidateAllForUser(user.getId(), now);

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        tokenRepository.save(new PasswordResetTokenEntity(user.getId(), sha256(token), now.plusMinutes(TOKEN_VALID_MINUTES)));

        notificationClient.send(NotificationType.PASSWORD_RESET, user.getEmail(), user.getFullName(), Map.of(
                "resetUrl", frontendUrl + "/reset-password?token=" + token,
                "validMinutes", String.valueOf(TOKEN_VALID_MINUTES)));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetTokenEntity resetToken = tokenRepository.findByTokenHash(sha256(token.trim()))
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> ApiException.badRequest(
                        "This reset link is invalid or has expired. Please request a new one."));
        UserEntity user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> ApiException.badRequest("This reset link is no longer valid."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.setUsedAt(now);
        tokenRepository.invalidateAllForUser(user.getId(), now);

        notificationClient.send(NotificationType.PASSWORD_CHANGED, user.getEmail(), user.getFullName(), Map.of());
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

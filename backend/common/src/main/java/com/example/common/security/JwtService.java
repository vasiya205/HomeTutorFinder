package com.example.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and verifies HS256 JSON Web Tokens. All services share the same JWT_SECRET,
 * so a token issued by the auth-service is trusted by every other service.
 */
@Service
public class JwtService {
    private static final String ISSUER = "home-tutor-finder";
    private static final Duration SERVICE_TOKEN_TTL = Duration.ofMinutes(5);

    private final SecretKey signingKey;
    private final Duration userTokenTtl;

    public JwtService(@Value("${jwt.secret:}") String secret,
                      @Value("${jwt.expiration-hours:10}") long expirationHours) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Add JWT_SECRET=<long random text> to the .env file in the project root.");
        }
        if (secret.length() < 16) {
            throw new IllegalStateException("JWT_SECRET must be at least 16 characters long.");
        }
        // Derive a fixed 256-bit key from the configured secret so any secret length works with HS256.
        this.signingKey = Keys.hmacShaKeyFor(sha256(secret));
        this.userTokenTtl = Duration.ofHours(expirationHours);
    }

    public String generateToken(Long userId, String email, String fullName, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("fullName", fullName)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(userTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    /** Short-lived token one microservice uses to call another microservice's internal API. */
    public String generateServiceToken(String serviceName) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject("0")
                .claim("fullName", serviceName)
                .claim("role", JwtPrincipal.ROLE_SERVICE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(SERVICE_TOKEN_TTL)))
                .signWith(signingKey)
                .compact();
    }

    /** Returns the caller if the token is authentic and not expired; otherwise empty. */
    public Optional<JwtPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String role = claims.get("role", String.class);
            if (role == null || claims.getSubject() == null) {
                return Optional.empty();
            }
            return Optional.of(new JwtPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("fullName", String.class),
                    role));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public long userTokenTtlSeconds() {
        return userTokenTtl.toSeconds();
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}

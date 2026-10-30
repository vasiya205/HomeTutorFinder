package com.example.common.security;

/**
 * The authenticated caller, rebuilt from a verified JWT. Every service uses this
 * instead of hitting the auth database on each request.
 */
public record JwtPrincipal(Long userId, String email, String fullName, String role) {

    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_TUTOR = "TUTOR";
    public static final String ROLE_ADMIN = "ADMIN";
    /** Used only for service-to-service calls (never issued to a browser). */
    public static final String ROLE_SERVICE = "SERVICE";

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean isService() {
        return ROLE_SERVICE.equals(role);
    }
}

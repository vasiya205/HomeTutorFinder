package com.example.authservice.api;

import com.example.authservice.service.AuthService;
import com.example.common.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return authService.listUsers();
    }

    @PatchMapping("/{userId}/enabled")
    public AdminUserResponse setEnabled(@AuthenticationPrincipal JwtPrincipal admin,
                                        @PathVariable Long userId,
                                        @Valid @RequestBody AccountEnabledRequest request) {
        return authService.setAccountEnabled(admin.userId(), userId, request.enabled());
    }
}

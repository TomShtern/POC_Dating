package com.dating.user.controller;

import com.dating.user.dto.request.LoginRequest;
import com.dating.user.dto.request.RefreshTokenRequest;
import com.dating.user.dto.request.RegisterRequest;
import com.dating.user.dto.response.AuthResponse;
import com.dating.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for authentication operations.
 * Handles registration, login, token refresh, and logout.
 */
@RestController
@RequestMapping("/api/users/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user.
     *
     * @param request Registration details
     * @return 201 Created with auth response
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("Registration request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user with email and password.
     *
     * @param request Login credentials
     * @return 200 OK with auth response
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param request Refresh token
     * @return 200 OK with new auth response
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh request");
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout user by revoking all refresh tokens.
     *
     * @param request HTTP request containing user ID
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        if (userId != null) {
            authService.logout(userId);
        }
        return ResponseEntity.noContent().build();
    }
}

package com.dating.chat.controller;

import com.dating.chat.service.PushNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for push notification registration.
 */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@Slf4j
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;

    /**
     * Register a push token for the current user.
     */
    @PostMapping("/register")
    public ResponseEntity<Void> registerToken(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody RegisterTokenRequest request) {

        pushNotificationService.registerPushToken(userIdHeader, request.token(), request.platform());
        return ResponseEntity.ok().build();
    }

    /**
     * Unregister a push token.
     */
    @DeleteMapping("/unregister")
    public ResponseEntity<Void> unregisterToken(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam String platform) {

        pushNotificationService.unregisterPushToken(userIdHeader, platform);
        return ResponseEntity.noContent().build();
    }

    /**
     * Request DTO for token registration.
     */
    public record RegisterTokenRequest(
            @NotBlank(message = "Token is required")
            @Size(max = 2000, message = "Token exceeds maximum length")
            String token,

            @NotBlank(message = "Platform is required")
            @Pattern(regexp = "^(fcm|web|apns)$", message = "Platform must be fcm, web, or apns")
            String platform
    ) {}
}

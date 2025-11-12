package com.dating.recommendation.controller;

import com.dating.recommendation.dto.PreferencesDTO;
import com.dating.recommendation.dto.UpdatePreferencesRequest;
import com.dating.recommendation.service.PreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user preferences endpoints.
 *
 * Endpoints:
 * - GET /api/preferences - Get current user's preferences
 * - PUT /api/preferences - Update current user's preferences
 * - POST /api/preferences - Create preferences (with defaults if not provided)
 * - DELETE /api/preferences - Delete current user's preferences
 */
@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
@Slf4j
public class PreferencesController {

    private final PreferencesService preferencesService;

    /**
     * Get the authenticated user's preferences.
     *
     * @param authentication Spring Security authentication object
     * @return user's preferences
     */
    @GetMapping
    public ResponseEntity<PreferencesDTO> getPreferences(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/preferences - User: {}", userId);

        try {
            PreferencesDTO preferences = preferencesService.getPreferences(userId);
            return ResponseEntity.ok(preferences);
        } catch (IllegalArgumentException e) {
            log.warn("Preferences not found for user: {}", userId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update the authenticated user's preferences.
     * Creates preferences if they don't exist.
     *
     * @param authentication Spring Security authentication object
     * @param request the preferences update request
     * @return updated preferences
     */
    @PutMapping
    public ResponseEntity<PreferencesDTO> updatePreferences(
            Authentication authentication,
            @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("PUT /api/preferences - User: {}", userId);

        try {
            PreferencesDTO preferences = preferencesService.updatePreferences(userId, request);
            return ResponseEntity.ok(preferences);
        } catch (IllegalArgumentException e) {
            log.error("Invalid preferences update request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create default preferences for the authenticated user.
     *
     * @param authentication Spring Security authentication object
     * @return created preferences
     */
    @PostMapping
    public ResponseEntity<PreferencesDTO> createDefaultPreferences(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("POST /api/preferences - User: {}", userId);

        try {
            PreferencesDTO preferences = preferencesService.createDefaultPreferences(userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(preferences);
        } catch (IllegalArgumentException e) {
            log.warn("Preferences already exist for user: {}", userId);
            // If preferences already exist, return them
            PreferencesDTO existingPreferences = preferencesService.getPreferences(userId);
            return ResponseEntity.ok(existingPreferences);
        }
    }

    /**
     * Delete the authenticated user's preferences.
     *
     * @param authentication Spring Security authentication object
     * @return no content
     */
    @DeleteMapping
    public ResponseEntity<Void> deletePreferences(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("DELETE /api/preferences - User: {}", userId);

        preferencesService.deletePreferences(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if the authenticated user has preferences.
     *
     * @param authentication Spring Security authentication object
     * @return boolean indicating if preferences exist
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> hasPreferences(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/preferences/exists - User: {}", userId);

        boolean exists = preferencesService.hasPreferences(userId);
        return ResponseEntity.ok(exists);
    }
}

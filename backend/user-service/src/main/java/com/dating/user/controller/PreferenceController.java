package com.dating.user.controller;

import com.dating.user.dto.request.UpdatePreferencesRequest;
import com.dating.user.dto.response.PreferencesResponse;
import com.dating.user.service.PreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for user preferences management.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class PreferenceController {

    private final PreferenceService preferenceService;

    /**
     * Get user preferences.
     *
     * @param userId User UUID
     * @return 200 OK with preferences response
     */
    @GetMapping("/{userId}/preferences")
    public ResponseEntity<PreferencesResponse> getPreferences(@PathVariable UUID userId) {
        log.debug("Get preferences request for user: {}", userId);
        PreferencesResponse response = preferenceService.getPreferences(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update user preferences.
     *
     * @param userId User UUID
     * @param request Updated preferences data
     * @return 200 OK with updated preferences response
     */
    @PutMapping("/{userId}/preferences")
    public ResponseEntity<PreferencesResponse> updatePreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        log.debug("Update preferences request for user: {}", userId);
        PreferencesResponse response = preferenceService.updatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }
}

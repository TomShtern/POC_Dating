package com.dating.match.controller;

import com.dating.match.dto.request.SwipeRequest;
import com.dating.match.dto.response.SwipeResponse;
import com.dating.match.service.SwipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for swipe operations.
 */
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Slf4j
public class SwipeController {

    private final SwipeService swipeService;

    /**
     * Record a swipe action.
     *
     * @param userId User ID from X-User-Id header
     * @param request Swipe request
     * @return Swipe response (201 Created, or 200 OK if match)
     */
    @PostMapping("/swipes")
    public ResponseEntity<SwipeResponse> recordSwipe(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SwipeRequest request) {
        log.debug("Swipe request from user {}", userId);

        SwipeResponse response = swipeService.recordSwipe(userId, request);

        // Return 200 if match was detected, 201 otherwise
        HttpStatus status = response.isMatch() ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(response);
    }
}

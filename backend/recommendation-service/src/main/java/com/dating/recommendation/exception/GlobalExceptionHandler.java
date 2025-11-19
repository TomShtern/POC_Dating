package com.dating.recommendation.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * GLOBAL EXCEPTION HANDLER
 * ============================================================================
 *
 * PURPOSE:
 * Catches all exceptions and converts them to standardized error responses.
 * Ensures consistent error format across all endpoints.
 *
 * ERROR RESPONSE FORMAT:
 * {
 *   "timestamp": "2024-01-15T10:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "User not found: abc-123",
 *   "path": "/api/recommendations/users/abc-123"
 * }
 *
 * HOW TO ADD NEW EXCEPTION HANDLERS:
 * 1. Create custom exception class
 * 2. Add @ExceptionHandler method here
 * 3. Return appropriate HTTP status
 *
 * ============================================================================
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle UserNotFoundException.
     * Maps to HTTP 404 Not Found.
     *
     * @param ex The UserNotFoundException that was thrown
     * @return Error response with 404 status
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle invalid method arguments (e.g., malformed UUID).
     * Maps to HTTP 400 Bad Request.
     *
     * @param ex The MethodArgumentTypeMismatchException that was thrown
     * @return Error response with 400 status
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid argument: {} - {}", ex.getName(), ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", String.format("Invalid value for parameter '%s': %s",
                ex.getName(), ex.getValue()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle IllegalArgumentException.
     * Maps to HTTP 400 Bad Request.
     *
     * @param ex The IllegalArgumentException that was thrown
     * @return Error response with 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle all other exceptions.
     * Maps to HTTP 500 Internal Server Error.
     *
     * @param ex The Exception that was thrown
     * @return Error response with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Log full stack trace for debugging, but don't expose details to client
        log.error("Unexpected error occurred", ex);

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("error", "Internal Server Error");
        error.put("message", "An unexpected error occurred");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

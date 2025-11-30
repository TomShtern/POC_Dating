package com.dating.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response DTO for REST API errors.
 * Used across all microservices for consistent error formatting.
 *
 * @param timestamp When the error occurred
 * @param status HTTP status code
 * @param error Error type/code
 * @param message Human-readable error message
 * @param path Request path that caused the error
 * @param validationErrors Field-level validation errors (optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    /**
     * Create an ErrorResponse with timestamp automatically set to now.
     *
     * @param status HTTP status code
     * @param error Error type/code
     * @param message Human-readable error message
     * @param path Request path
     * @return New ErrorResponse instance
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    /**
     * Create an ErrorResponse with validation errors.
     *
     * @param status HTTP status code
     * @param error Error type/code
     * @param message Human-readable error message
     * @param path Request path
     * @param validationErrors Field-level validation errors
     * @return New ErrorResponse instance
     */
    public static ErrorResponse withValidationErrors(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> validationErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, path, validationErrors);
    }
}

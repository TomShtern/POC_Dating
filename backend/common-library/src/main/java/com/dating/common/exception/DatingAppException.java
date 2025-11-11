package com.dating.common.exception;

/**
 * Base Exception for Dating App
 *
 * PURPOSE: Root exception class for all application-specific errors
 *
 * USAGE:
 * Extend this for specific error scenarios:
 * - UserNotFoundException extends DatingAppException
 * - UnauthorizedException extends DatingAppException
 * - ValidationException extends DatingAppException
 * - MatchNotFoundException extends DatingAppException
 *
 * FIELDS TO IMPLEMENT:
 * - errorCode: Machine-readable error code (e.g., "USER_NOT_FOUND")
 * - message: Human-readable error message
 * - statusCode: HTTP status code (400, 401, 404, 500, etc.)
 * - cause: Root cause exception
 *
 * USAGE EXAMPLE:
 * throw new DatingAppException("USER_NOT_FOUND", "User with id 123 not found", 404);
 *
 * API RESPONSE:
 * {
 *   "error": "USER_NOT_FOUND",
 *   "message": "User with id 123 not found",
 *   "timestamp": "2025-11-11T10:00:00Z",
 *   "path": "/api/users/123"
 * }
 */
public class DatingAppException extends RuntimeException {
    // TODO: Add fields: errorCode, statusCode
    // TODO: Add constructors with various parameter combinations
    // TODO: Add getters for errorCode and statusCode
    // TODO: Consider adding metadata (field names, values for validation errors)
}

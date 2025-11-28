package com.dating.common.exception;

import java.util.Collections;
import java.util.Map;

/**
 * Exception thrown when input validation fails.
 *
 * <p>Maps to HTTP 400 Bad Request status code.</p>
 *
 * <p>Can include field-level validation errors for detailed feedback.</p>
 */
public class ValidationException extends DatingException {

    private static final long serialVersionUID = 1L;

    /**
     * Field-level validation errors.
     * Key: field name, Value: error message
     */
    private final Map<String, String> fieldErrors;

    /**
     * Constructs a new ValidationException with the specified message.
     *
     * @param message The detail message
     */
    public ValidationException(String message) {
        super(message);
        this.fieldErrors = Collections.emptyMap();
    }

    /**
     * Constructs a new ValidationException with field errors.
     *
     * @param message The detail message
     * @param fieldErrors Map of field names to error messages
     */
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? Map.copyOf(fieldErrors) : Collections.emptyMap();
    }

    /**
     * Constructs a new ValidationException for a single field.
     *
     * @param field Field name
     * @param error Error message
     */
    public ValidationException(String field, String error) {
        super(String.format("Validation failed for field '%s': %s", field, error));
        this.fieldErrors = Map.of(field, error);
    }

    /**
     * Get the field-level validation errors.
     *
     * @return Immutable map of field errors
     */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * Check if there are field-level errors.
     *
     * @return true if there are field errors
     */
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }

    /**
     * Create exception for invalid age range.
     *
     * @return New ValidationException instance
     */
    public static ValidationException invalidAgeRange() {
        return new ValidationException("minAge", "Minimum age cannot be greater than maximum age");
    }

    /**
     * Create exception for invalid distance.
     *
     * @return New ValidationException instance
     */
    public static ValidationException invalidDistance() {
        return new ValidationException("maxDistance", "Maximum distance must be between 1 and 500 km");
    }
}

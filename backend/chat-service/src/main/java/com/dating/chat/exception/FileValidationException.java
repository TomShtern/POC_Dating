package com.dating.chat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when file validation fails.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FileValidationException extends RuntimeException {

    public FileValidationException(String message) {
        super(message);
    }
}

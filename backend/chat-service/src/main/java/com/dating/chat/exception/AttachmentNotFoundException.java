package com.dating.chat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Exception thrown when an attachment is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AttachmentNotFoundException extends RuntimeException {

    public AttachmentNotFoundException(UUID attachmentId) {
        super(String.format("Attachment not found: %s", attachmentId));
    }
}

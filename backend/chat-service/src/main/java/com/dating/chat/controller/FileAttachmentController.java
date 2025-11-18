package com.dating.chat.controller;

import com.dating.chat.model.FileAttachment;
import com.dating.chat.service.FileAttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST Controller for file attachment operations.
 */
@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Slf4j
public class FileAttachmentController {

    private final FileAttachmentService attachmentService;

    /**
     * Upload a file attachment.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("file") MultipartFile file) {

        UUID userId = UUID.fromString(userIdHeader);
        FileAttachment attachment = attachmentService.uploadAttachment(userId, file);

        return ResponseEntity.ok(new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getStatus().name()
        ));
    }

    /**
     * Get attachment metadata.
     */
    @GetMapping("/{attachmentId}")
    public ResponseEntity<AttachmentResponse> getAttachment(
            @PathVariable UUID attachmentId) {

        FileAttachment attachment = attachmentService.getAttachment(attachmentId);

        return ResponseEntity.ok(new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getStatus().name()
        ));
    }

    /**
     * Download attachment content.
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable UUID attachmentId) {

        FileAttachment attachment = attachmentService.getAttachment(attachmentId);
        byte[] content = attachmentService.getFileContent(attachmentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .contentLength(content.length)
                .body(content);
    }

    /**
     * Delete an attachment.
     */
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID attachmentId) {

        UUID userId = UUID.fromString(userIdHeader);
        attachmentService.deleteAttachment(attachmentId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Response DTO for attachment operations.
     */
    public record AttachmentResponse(
            UUID id,
            String filename,
            String contentType,
            Long fileSize,
            String status
    ) {}
}

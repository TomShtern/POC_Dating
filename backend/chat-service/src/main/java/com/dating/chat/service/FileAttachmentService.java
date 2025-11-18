package com.dating.chat.service;

import com.dating.chat.exception.AttachmentNotFoundException;
import com.dating.chat.exception.FileValidationException;
import com.dating.chat.model.FileAttachment;
import com.dating.chat.repository.FileAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * File Attachment Service
 *
 * Handles file uploads, validation, and storage for chat attachments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileAttachmentService {

    private final FileAttachmentRepository attachmentRepository;

    @Value("${app.attachments.enabled:true}")
    private boolean attachmentsEnabled;

    @Value("${app.attachments.max-file-size:10485760}")
    private long maxFileSize;

    @Value("${app.attachments.allowed-types:image/jpeg,image/png,image/gif,video/mp4,audio/mpeg}")
    private String allowedTypes;

    @Value("${app.attachments.storage-path:/tmp/chat-attachments}")
    private String storagePath;

    /**
     * Upload a file attachment.
     */
    @Transactional
    public FileAttachment uploadAttachment(UUID userId, MultipartFile file) {
        if (!attachmentsEnabled) {
            throw new FileValidationException("File attachments are disabled");
        }

        // Validate file
        validateFile(file);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String storedFilename = UUID.randomUUID() + getFileExtension(originalFilename);

        // Create storage directory if needed
        Path storageDir = Paths.get(storagePath, userId.toString());
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            log.error("Failed to create storage directory: {}", storageDir, e);
            throw new FileValidationException("Failed to create storage directory");
        }

        // Save file to disk
        Path filePath = storageDir.resolve(storedFilename);
        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            log.error("Failed to save file: {}", filePath, e);
            throw new FileValidationException("Failed to save file");
        }

        // Create attachment record
        FileAttachment attachment = FileAttachment.builder()
                .uploadedBy(userId)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .storagePath(filePath.toString())
                .status(FileAttachment.AttachmentStatus.READY)
                .build();

        attachment = attachmentRepository.save(attachment);
        log.info("File uploaded: id={}, user={}, filename={}, size={}",
                attachment.getId(), userId, originalFilename, file.getSize());

        return attachment;
    }

    /**
     * Get attachment by ID.
     */
    @Transactional(readOnly = true)
    public FileAttachment getAttachment(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));
    }

    /**
     * Get attachment by ID and verify ownership.
     */
    @Transactional(readOnly = true)
    public FileAttachment getAttachmentForUser(UUID attachmentId, UUID userId) {
        return attachmentRepository.findByIdAndUploadedBy(attachmentId, userId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));
    }

    /**
     * Link attachment to a message.
     */
    @Transactional
    public void linkToMessage(UUID attachmentId, UUID messageId) {
        FileAttachment attachment = getAttachment(attachmentId);
        attachment.setMessageId(messageId);
        attachmentRepository.save(attachment);
        log.debug("Attachment {} linked to message {}", attachmentId, messageId);
    }

    /**
     * Get file content as bytes.
     */
    public byte[] getFileContent(UUID attachmentId) {
        FileAttachment attachment = getAttachment(attachmentId);
        try {
            return Files.readAllBytes(Paths.get(attachment.getStoragePath()));
        } catch (IOException e) {
            log.error("Failed to read file: {}", attachment.getStoragePath(), e);
            throw new FileValidationException("Failed to read file");
        }
    }

    /**
     * Delete an attachment.
     */
    @Transactional
    public void deleteAttachment(UUID attachmentId, UUID userId) {
        FileAttachment attachment = getAttachmentForUser(attachmentId, userId);

        // Delete file from disk
        try {
            Files.deleteIfExists(Paths.get(attachment.getStoragePath()));
        } catch (IOException e) {
            log.error("Failed to delete file: {}", attachment.getStoragePath(), e);
        }

        // Mark as deleted
        attachment.setStatus(FileAttachment.AttachmentStatus.DELETED);
        attachmentRepository.save(attachment);

        log.info("Attachment deleted: id={}, user={}", attachmentId, userId);
    }

    /**
     * Get attachments for a message.
     */
    @Transactional(readOnly = true)
    public List<FileAttachment> getAttachmentsForMessage(UUID messageId) {
        return attachmentRepository.findByMessageId(messageId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File is empty");
        }

        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new FileValidationException(
                    String.format("File size %d exceeds maximum allowed size %d", file.getSize(), maxFileSize));
        }

        // Check content type
        String contentType = file.getContentType();
        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        if (contentType == null || !allowed.contains(contentType.toLowerCase())) {
            throw new FileValidationException(
                    String.format("Content type '%s' is not allowed. Allowed types: %s", contentType, allowedTypes));
        }

        // Validate filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new FileValidationException("Invalid filename");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}

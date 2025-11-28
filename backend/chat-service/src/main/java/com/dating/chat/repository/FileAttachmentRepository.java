package com.dating.chat.repository;

import com.dating.chat.model.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for FileAttachment entities.
 */
@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, UUID> {

    /**
     * Find attachment by ID and uploader.
     */
    Optional<FileAttachment> findByIdAndUploadedBy(UUID id, UUID uploadedBy);

    /**
     * Find all attachments for a message.
     */
    List<FileAttachment> findByMessageId(UUID messageId);

    /**
     * Find all attachments uploaded by a user.
     */
    List<FileAttachment> findByUploadedBy(UUID userId);

    /**
     * Find attachments by status.
     */
    List<FileAttachment> findByStatus(FileAttachment.AttachmentStatus status);
}

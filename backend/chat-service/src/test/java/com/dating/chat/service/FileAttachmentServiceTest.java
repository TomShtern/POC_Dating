package com.dating.chat.service;

import com.dating.chat.exception.AttachmentNotFoundException;
import com.dating.chat.exception.FileValidationException;
import com.dating.chat.model.FileAttachment;
import com.dating.chat.repository.FileAttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileAttachmentServiceTest {

    @Mock
    private FileAttachmentRepository attachmentRepository;

    @Mock
    private MultipartFile multipartFile;

    @TempDir
    Path tempDir;

    private FileAttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        attachmentService = new FileAttachmentService(attachmentRepository);
        ReflectionTestUtils.setField(attachmentService, "attachmentsEnabled", true);
        ReflectionTestUtils.setField(attachmentService, "maxFileSize", 10485760L);
        ReflectionTestUtils.setField(attachmentService, "allowedTypes", "image/jpeg,image/png,image/gif");
        ReflectionTestUtils.setField(attachmentService, "storagePath", tempDir.toString());
    }

    @Test
    void uploadAttachment_Success() throws IOException {
        UUID userId = UUID.randomUUID();
        byte[] content = "test content".getBytes();

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        when(attachmentRepository.save(any(FileAttachment.class)))
                .thenAnswer(invocation -> {
                    FileAttachment att = invocation.getArgument(0);
                    att.setId(UUID.randomUUID());
                    return att;
                });

        FileAttachment result = attachmentService.uploadAttachment(userId, multipartFile);

        assertNotNull(result);
        assertEquals(userId, result.getUploadedBy());
        assertEquals("test.jpg", result.getOriginalFilename());
        assertEquals("image/jpeg", result.getContentType());
        assertEquals(FileAttachment.AttachmentStatus.READY, result.getStatus());
    }

    @Test
    void uploadAttachment_WhenDisabled_ThrowsException() {
        ReflectionTestUtils.setField(attachmentService, "attachmentsEnabled", false);

        assertThrows(FileValidationException.class, () ->
                attachmentService.uploadAttachment(UUID.randomUUID(), multipartFile));
    }

    @Test
    void uploadAttachment_EmptyFile_ThrowsException() {
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(FileValidationException.class, () ->
                attachmentService.uploadAttachment(UUID.randomUUID(), multipartFile));
    }

    @Test
    void uploadAttachment_FileTooLarge_ThrowsException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(20000000L);

        assertThrows(FileValidationException.class, () ->
                attachmentService.uploadAttachment(UUID.randomUUID(), multipartFile));
    }

    @Test
    void uploadAttachment_InvalidContentType_ThrowsException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("application/exe");

        assertThrows(FileValidationException.class, () ->
                attachmentService.uploadAttachment(UUID.randomUUID(), multipartFile));
    }

    @Test
    void uploadAttachment_InvalidFilename_ThrowsException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getOriginalFilename()).thenReturn("../test.jpg");

        assertThrows(FileValidationException.class, () ->
                attachmentService.uploadAttachment(UUID.randomUUID(), multipartFile));
    }

    @Test
    void getAttachment_Found_ReturnsAttachment() {
        UUID attachmentId = UUID.randomUUID();
        FileAttachment expected = FileAttachment.builder()
                .id(attachmentId)
                .originalFilename("test.jpg")
                .build();

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(expected));

        FileAttachment result = attachmentService.getAttachment(attachmentId);

        assertEquals(expected, result);
    }

    @Test
    void getAttachment_NotFound_ThrowsException() {
        UUID attachmentId = UUID.randomUUID();
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        assertThrows(AttachmentNotFoundException.class, () ->
                attachmentService.getAttachment(attachmentId));
    }

    @Test
    void getAttachmentForUser_Found_ReturnsAttachment() {
        UUID attachmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FileAttachment expected = FileAttachment.builder()
                .id(attachmentId)
                .uploadedBy(userId)
                .build();

        when(attachmentRepository.findByIdAndUploadedBy(attachmentId, userId))
                .thenReturn(Optional.of(expected));

        FileAttachment result = attachmentService.getAttachmentForUser(attachmentId, userId);

        assertEquals(expected, result);
    }

    @Test
    void linkToMessage_UpdatesAttachment() {
        UUID attachmentId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        FileAttachment attachment = FileAttachment.builder()
                .id(attachmentId)
                .build();

        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        attachmentService.linkToMessage(attachmentId, messageId);

        assertEquals(messageId, attachment.getMessageId());
        verify(attachmentRepository).save(attachment);
    }

    @Test
    void deleteAttachment_MarksAsDeleted() {
        UUID attachmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FileAttachment attachment = FileAttachment.builder()
                .id(attachmentId)
                .uploadedBy(userId)
                .storagePath(tempDir.resolve("test.jpg").toString())
                .build();

        when(attachmentRepository.findByIdAndUploadedBy(attachmentId, userId))
                .thenReturn(Optional.of(attachment));

        attachmentService.deleteAttachment(attachmentId, userId);

        assertEquals(FileAttachment.AttachmentStatus.DELETED, attachment.getStatus());
        verify(attachmentRepository).save(attachment);
    }
}

package com.dating.ui.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @NotBlank(message = "Message ID is required")
    private String id;

    @NotBlank(message = "Conversation ID is required")
    private String conversationId;

    @NotBlank(message = "Sender ID is required")
    private String senderId;

    private String senderName;

    @NotBlank(message = "Message text is required")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String text;

    @NotNull(message = "Created timestamp is required")
    private Instant createdAt;

    private MessageStatus status;
}

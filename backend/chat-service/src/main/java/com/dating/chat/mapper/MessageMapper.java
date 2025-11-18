package com.dating.chat.mapper;

import com.dating.chat.dto.response.MessageResponse;
import com.dating.chat.model.Message;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Message entity to DTOs.
 */
@Component
public class MessageMapper {

    /**
     * Convert Message entity to MessageResponse DTO.
     *
     * @param message Message entity
     * @return MessageResponse DTO
     */
    public MessageResponse toMessageResponse(Message message) {
        return toMessageResponse(message, null);
    }

    /**
     * Convert Message entity to MessageResponse DTO with sender name.
     *
     * @param message Message entity
     * @param senderName Sender's display name (can be null)
     * @return MessageResponse DTO
     */
    public MessageResponse toMessageResponse(Message message, String senderName) {
        if (message == null) {
            return null;
        }

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getMatchId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .content(message.getContent())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .build();
    }
}

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
        if (message == null) {
            return null;
        }

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getMatchId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .status(message.getStatus())
                .sentAt(message.getCreatedAt())
                .deliveredAt(message.getDeliveredAt())
                .readAt(message.getReadAt())
                .build();
    }
}

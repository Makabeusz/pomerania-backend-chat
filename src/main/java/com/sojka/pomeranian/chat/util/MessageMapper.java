package com.sojka.pomeranian.chat.util;

import com.sojka.pomeranian.chat.dto.ChatMessageResponse;
import com.sojka.pomeranian.chat.dto.ChatUser;
import com.sojka.pomeranian.chat.dto.MessageType;
import com.sojka.pomeranian.chat.model.Message;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class MessageMapper {

    /**
     * todo: create DateTimeFormatter and use here
     */
    public static ChatMessageResponse toDto(Message message) {
        return ChatMessageResponse.builder()
                .roomId(message.getRoomId())
                .createdAt(LocalDateTime.ofInstant(message.getCreatedAt(), ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .messageId(message.getMessageId())
                .sender(new ChatUser(message.getProfileId(), message.getUsername()))
                .recipient(new ChatUser(message.getRecipientProfileId(), message.getRecipientUsername()))
                .content(message.getContent())
                .type(message.getMessageType() != null ? MessageType.valueOf(message.getMessageType()): null)
                .resourceId(message.getResourceId())
                .threadId(message.getThreadId())
                .editedAt(message.getEditedAt())
                .deletedAt(message.getDeletedAt())
                .pinned(message.getPinned())
                .metadata(message.getMetadata())
                .build();
    }
}

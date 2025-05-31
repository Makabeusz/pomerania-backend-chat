package com.sojka.pomeranian.chat.util;

import com.sojka.pomeranian.chat.dto.ChatMessageResponse;
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
                        .format(DateTimeFormatter.ISO_DATE))
                .messageId(message.getMessageId())
                .profileId(message.getProfileId())
                .username(message.getUsername())
                .content(message.getContent())
                .type(message.getMessageType())
                .resourceId(message.getResourceId())
                .threadId(message.getThreadId())
                .editedAt(message.getEditedAt())
                .deletedAt(message.getDeletedAt())
                .pinned(message.getPinned())
                .metadata(message.getMetadata())
                .build();
    }
}

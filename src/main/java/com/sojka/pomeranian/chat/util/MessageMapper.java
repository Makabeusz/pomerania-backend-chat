package com.sojka.pomeranian.chat.util;

import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.ChatUser;
import com.sojka.pomeranian.chat.model.Message;

import static com.sojka.pomeranian.chat.util.CommonUtils.formatToDateString;

public final class MessageMapper {

    public static ChatMessagePersisted toDto(Message message) {
        return ChatMessagePersisted.builder()
                .roomId(message.getRoomId())
                .createdAt(formatToDateString(message.getCreatedAt()))
                .sender(new ChatUser(message.getProfileId(), message.getUsername()))
                .recipient(new ChatUser(message.getRecipientProfileId(), message.getRecipientUsername()))
                .content(message.getContent())
                .resourceId(message.getResourceId())
                .threadId(message.getThreadId())
                .editedAt(message.getEditedAt())
                .deletedAt(message.getDeletedAt())
                .pinned(message.getPinned())
                .readAt(formatToDateString(message.getReadAt()))
                .metadata(message.getMetadata())
                .build();
    }
}

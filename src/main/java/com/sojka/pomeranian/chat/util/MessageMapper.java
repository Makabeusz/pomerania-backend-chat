package com.sojka.pomeranian.chat.util;

import com.datastax.oss.driver.api.core.cql.Row;
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

    public static Message fromAstraRow(Row row) {
        return Message.builder()
                .roomId(row.getString("room_id"))
                .createdAt(row.getInstant("created_at"))
                .profileId(row.getString("profile_id"))
                .username(row.getString("username"))
                .recipientUsername(row.getString("recipient_username"))
                .recipientProfileId(row.getString("recipient_profile_id"))
                .content(row.getString("content"))
                .resourceId(row.getString("resource_id"))
                .threadId(row.getString("thread_id"))
                .editedAt(row.getString("edited_at"))
                .deletedAt(row.getString("deleted_at"))
                .pinned(row.getBoolean("pinned"))
                .readAt(row.getInstant("read_at"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .build();
    }
}

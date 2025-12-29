package com.sojka.pomeranian.chat.util.mapper;

import com.datastax.oss.driver.api.core.cql.Row;
import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.ChatUser;
import com.sojka.pomeranian.chat.model.Message;

import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;


public final class MessageMapper {

    private MessageMapper() {
    }

    public static ChatMessagePersisted toDto(Message message) {
        return ChatMessagePersisted.builder()
                .roomId(message.getRoomId())
                .createdAt(toDateString(message.getCreatedAt()))
                // TODO: check if valid - hardcoded null image192
                .sender(new ChatUser(message.getProfileId(), message.getUsername(), null))
                .recipient(new ChatUser(message.getRecipientProfileId(), message.getRecipientUsername(), null))
                .content(message.getContent())
                .resourceId(message.getResourceId())
                .resourceType(message.getResourceType())
                .editedAt(message.getEditedAt())
                .readAt(toDateString(message.getReadAt()))
                .metadata(message.getMetadata())
                .build();
    }

    public static Message fromAstraRow(Row row) {
        return Message.builder()
                .roomId(row.getString("room_id"))
                .createdAt(row.getInstant("created_at"))
                .profileId(row.getUuid("profile_id"))
                .username(row.getString("username"))
                .recipientUsername(row.getString("recipient_username"))
                .recipientProfileId(row.getUuid("recipient_profile_id"))
                .content(row.getString("content"))
                .resourceId(row.getUuid("resource_id"))
                .resourceType(row.getString("resource_type"))
                .editedAt(row.getString("edited_at"))
                .readAt(row.getInstant("read_at"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .build();
    }

    public static String roomIdFromAstraRow(Row row) {
        return row.getString("room_id");
    }
}

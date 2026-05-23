package com.sojka.pomeranian.chat.util.mapper;

import com.datastax.oss.driver.api.core.cql.Row;
import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.UserId;
import com.sojka.pomeranian.chat.model.Message;

import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static ChatMessagePersisted toDto(Message message) {
        return ChatMessagePersisted.builder()
                .roomId(message.getRoomId())
                .createdAt(toDateString(message.getCreatedAt()))
                .sender(new UserId(message.getProfileId()))
                .recipient(new UserId(message.getRecipientProfileId()))
                .content(message.getContent())
                .resourceId(message.getResourceId())
                .resourceType(message.getResourceType())
                .resourceHeight(message.getResourceHeight())
                .resourceWidth(message.getResourceWidth())
                .thumbnailId(message.getThumbnailId())
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
                .content(row.getString("content"))
                .resourceId(row.getUuid("resource_id"))
                .resourceType(row.getString("resource_type"))
                .resourceHeight(row.getInt("resource_height"))
                .resourceWidth(row.getInt("resource_width"))
                .thumbnailId(row.getUuid("thumbnail_id"))
                .editedAt(row.getString("edited_at"))
                .readAt(row.getInstant("read_at"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .build();
    }

}

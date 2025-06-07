package com.sojka.pomeranian.chat.dto;

import com.sojka.pomeranian.chat.model.Message;

import java.time.Instant;

/**
 * The DTO containing {@link Message} primary key.
 */
public record MessageKey(
        String roomId,
        Instant createdAt,
        String profileId
) {

    public MessageKey(String roomId, Instant createdAt, String profileId) {
        this.roomId = roomId;
        this.createdAt = createdAt;
        this.profileId = profileId;
    }

    public MessageKey(Message message) {
        this(message.getRoomId(), message.getCreatedAt(), message.getProfileId());
    }

}

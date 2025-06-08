package com.sojka.pomeranian.chat.dto;

import com.sojka.pomeranian.chat.model.Message;

import java.time.Instant;
import java.util.List;

/**
 * The DTO containing {@link Message} primary key.
 */
public record MessageKey(
        String roomId,
        List<Instant> createdAt,
        String profileId
) {

    public MessageKey(String roomId, List<Instant> createdAt, String profileId) {
        this.roomId = roomId;
        this.createdAt = createdAt;
        this.profileId = profileId;
    }

    public MessageKey(Message message) {
        this(message.getRoomId(), List.of(message.getCreatedAt()), message.getProfileId());
    }

}

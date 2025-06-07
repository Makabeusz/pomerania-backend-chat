package com.sojka.pomeranian.chat.dto;

import com.sojka.pomeranian.chat.util.CommonUtils;

public record ChatReadResponse(
        String roomId,
        String createdAt,
        String profileId,
        String readAt
) {

    public ChatReadResponse(String roomId, String createdAt, String profileId, String readAt) {
        this.roomId = roomId;
        this.createdAt = createdAt;
        this.profileId = profileId;
        this.readAt = readAt;
    }

    public ChatReadResponse(MessageKey key, String readAt) {
        this(key.roomId(), CommonUtils.formatToDateString(key.createdAt()), key.profileId(), readAt);
    }
}

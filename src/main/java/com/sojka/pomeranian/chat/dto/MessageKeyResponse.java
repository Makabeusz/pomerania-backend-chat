package com.sojka.pomeranian.chat.dto;

public record MessageKeyResponse(
        String roomId,
        String createdAt,
        String profileId
) {
}

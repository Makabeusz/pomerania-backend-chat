package com.sojka.pomeranian.chat.dto;

public record ReadMessageDto(
        String roomId,
        String createdAt
) {
}

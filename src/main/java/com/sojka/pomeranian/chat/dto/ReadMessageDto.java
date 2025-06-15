package com.sojka.pomeranian.chat.dto;

import java.util.List;

public record ReadMessageDto(
        String roomId,
        List<String> createdAt
) {
}

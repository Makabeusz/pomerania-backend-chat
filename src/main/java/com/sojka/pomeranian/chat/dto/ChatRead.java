package com.sojka.pomeranian.chat.dto;

import java.util.List;

public record ChatRead(
        List<String> createdAt,
        String readAt
) {
}

package com.sojka.pomeranian.chat.dto;

import lombok.NonNull;

import java.util.List;

public record ChatRead(
        List<String> createdAt,
        String readAt
) {

    public ChatRead(@NonNull List<String> createdAt, @NonNull String readAt) {
        if (createdAt.isEmpty()) {
            throw new IllegalArgumentException("Cannot pass empty list");
        }
        this.createdAt = createdAt;
        this.readAt = readAt;
    }
}

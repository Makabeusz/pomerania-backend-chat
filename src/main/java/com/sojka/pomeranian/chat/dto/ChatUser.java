package com.sojka.pomeranian.chat.dto;

import java.util.UUID;

public record ChatUser(UUID id, String username, UUID image192) {
    public static ChatUser getEmpty() {
        return new ChatUser(null, null, null);
    }
}

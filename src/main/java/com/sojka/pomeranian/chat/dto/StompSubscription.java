package com.sojka.pomeranian.chat.dto;


public record StompSubscription(
        Type type,
        String id
) {

    public enum Type {

        CHAT, CHAT_NOTIFICATIONS
    }
}

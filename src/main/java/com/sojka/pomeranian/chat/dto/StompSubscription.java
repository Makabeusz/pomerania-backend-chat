package com.sojka.pomeranian.chat.dto;


/**
 * @param type Subscription type
 * @param id Might be room_id, profile_id or other
 */
public record StompSubscription(
        Type type,
        String id
) {

    public enum Type {

        CHAT, CHAT_NOTIFICATIONS, POST_COMMENTS
    }
}

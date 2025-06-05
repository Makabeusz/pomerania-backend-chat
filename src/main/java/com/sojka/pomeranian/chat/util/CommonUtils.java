package com.sojka.pomeranian.chat.util;

import com.sojka.pomeranian.chat.dto.ChatMessage;

public final class CommonUtils {

    /**
     * @see CommonUtils#generateRoomId(String, String)
     */
    public static String generateRoomId(ChatMessage chatMessage) {
        return CommonUtils.generateRoomId(chatMessage.getSender().id(), chatMessage.getRecipient().id());
    }

    /**
     * Generates consistent private message room ID.<br>
     * Sorts the IDs in alphabetic order and combine them with a colon ':'.
     */
    public static String generateRoomId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0 ? userId1 + ":" + userId2 : userId2 + ":" + userId1;
    }
}

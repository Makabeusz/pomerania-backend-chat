package com.sojka.pomeranian.chat.util;

import com.sojka.pomeranian.chat.dto.ChatMessage;

public final class CommonUtils {

    /**
     * @see com.sojka.pomeranian.lib.util.CommonUtils#generateRoomId(String, String)
     */
    public static String generateRoomId(ChatMessage chatMessage) {
        return com.sojka.pomeranian.lib.util.CommonUtils.generateRoomId(chatMessage.getSender().id(), chatMessage.getRecipient().id());
    }

}

package com.sojka.pomeranian.chat.util;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.model.Conversation;

public final class CommonUtils {

    /**
     * @see com.sojka.pomeranian.lib.util.CommonUtils#generateRoomId(String, String)
     */
    public static String generateRoomId(ChatMessage chatMessage) {
        return com.sojka.pomeranian.lib.util.CommonUtils.generateRoomId(chatMessage.getSender().id(), chatMessage.getRecipient().id());
    }

    /**
     * @see com.sojka.pomeranian.lib.util.CommonUtils#generateRoomId(String, String)
     */
    public static String generateRoomId(Conversation conversation) {
        return com.sojka.pomeranian.lib.util.CommonUtils.generateRoomId(
                conversation.getId().getUserId(), conversation.getId().getRecipientId()
        );
    }

}

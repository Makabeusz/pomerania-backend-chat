package com.sojka.pomeranian.chat.util;

import com.sojka.pomeranian.chat.model.Message;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

public class TestUtils {

    public static Comparator<LocalDateTime> timestampComparator() {
        return (o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null && o2 != null) {
                return 1;
            } else if (o1 != null && o2 == null) {
                return -1;
            }
            long diffInSeconds = Math.abs(Duration.between(o1, o2).getSeconds());
            return diffInSeconds <= 3 ? 0 : o1.compareTo(o2);
        };
    }

    public static Message createChatMessage(String roomId, String content, String senderId, String recipientId, Instant createdAt) {
        Message message = new Message();
        message.setRoomId(roomId);
        message.setMessageId(UUID.randomUUID().toString());
        message.setCreatedAt(createdAt);
        message.setProfileId(senderId);
        message.setUsername("User" + senderId);
        message.setRecipientProfileId(recipientId);
        message.setRecipientUsername("User" + recipientId);
        message.setContent(content);
        message.setMessageType("CHAT");
        return message;
    }
}

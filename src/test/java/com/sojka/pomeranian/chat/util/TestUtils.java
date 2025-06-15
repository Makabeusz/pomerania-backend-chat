package com.sojka.pomeranian.chat.util;

import com.sojka.pomeranian.chat.dto.Pagination;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.util.mapper.PaginationMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;

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
        message.setCreatedAt(createdAt);
        message.setProfileId(senderId);
        message.setUsername("User" + senderId);
        message.setRecipientProfileId(recipientId);
        message.setRecipientUsername("User" + recipientId);
        message.setContent(content);
        return message;
    }

    public static String paginationString(int pageNumber, int pageSize) {
        return PaginationMapper.toEncodedString(new Pagination(pageNumber, pageSize));
    }

}

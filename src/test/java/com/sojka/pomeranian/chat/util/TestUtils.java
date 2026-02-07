package com.sojka.pomeranian.chat.util;

import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.sojka.pomeranian.astra.connection.Connector;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.lib.dto.NotificationType;
import com.sojka.pomeranian.lib.util.JsonUtils;
import com.sojka.pomeranian.notification.model.NotificationModel;
import com.sojka.pomeranian.notification.model.ReadNotification;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
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

    public static Message createChatMessage(String roomId, String content, UUID senderId, UUID recipientId, Instant createdAt) {
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

    public static NotificationModel getNotification(Connector connector, UUID profileId, Instant createdAt, String type) {
        SimpleStatement selectNotification = SimpleStatement.newInstance(
                "SELECT * FROM notifications.notifications WHERE profile_id = ? AND created_at = ? AND type = ?",
                profileId, createdAt, type
        );
        var row = connector.getSession().execute(selectNotification).one();
        if (row == null) {
            System.out.println("null result set");
            return null;
        }
        return NotificationModel.builder()
                .profileId(row.getUuid("profile_id"))
                .createdAt(row.getInstant("created_at"))
                .type(NotificationType.valueOf(row.getString("type")))
                .body(JsonUtils.writeToString(Map.of("content", "content")))
                .build();
    }

    public static ReadNotification getReadNotification(Connector connector, UUID profileId, Instant createdAt, String type) {
        SimpleStatement selectNotification = SimpleStatement.newInstance(
                "SELECT * FROM notifications.read_notifications WHERE profile_id = ? AND created_at = ? AND type = ?",
                profileId, createdAt, type
        );
        var row = connector.getSession().execute(selectNotification).one();
        return ReadNotification.builder()
                .profileId(row.getUuid("profile_id"))
                .createdAt(row.getInstant("created_at"))
                .type(NotificationType.valueOf(row.getString("type")))
                .readAt(row.getInstant("read_at"))
                .body(row.getString("body"))
                .build();
    }

}

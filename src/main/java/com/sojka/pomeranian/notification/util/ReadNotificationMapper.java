package com.sojka.pomeranian.notification.util;

import com.datastax.oss.driver.api.core.cql.Row;
import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.lib.dto.NotificationType;
import com.sojka.pomeranian.lib.dto.UserData;
import com.sojka.pomeranian.lib.util.JsonUtils;
import com.sojka.pomeranian.notification.model.ReadNotification;
import com.sojka.pomeranian.security.model.Role;

import java.time.Instant;

import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toInstant;

public final class ReadNotificationMapper {

    private ReadNotificationMapper() {
    }

    public static Notification<Object> toDto(ReadNotification notification) {
        if (notification == null) {
            return null;
        }
        return Notification.builder()
                .profileId(notification.getProfileId())
                .createdAt(toDateString(notification.getCreatedAt()))
                .type(notification.getType())
                .readAt(toDateString(notification.getReadAt()))
                .body(JsonUtils.readMap(notification.getBody()))
                .sender(UserData.builder()
                        .id(notification.getSenderId())
                        .username(notification.getSenderUsername())
                        .image192(notification.getSenderImage192())
                        .gender(notification.getSenderGender())
                        .role(notification.getSenderRole())
                        .build())
                .build();
    }

    public static ReadNotification toReadNotificationDomain(Notification<Object> notification, Instant readAt) {
        if (notification == null) {
            return null;
        }
        return ReadNotification.builder()
                .profileId(notification.getProfileId())
                .createdAt(toInstant(notification.getCreatedAt()))
                .type(notification.getType())
                .readAt(readAt)
                .body(JsonUtils.writeToString(notification.getBody()))
                .senderId(notification.getSender().getId())
                .senderUsername(notification.getSender().getUsername())
                .senderImage192(notification.getSender().getImage192())
                .senderGender(notification.getSender().getGender())
                .senderRole(notification.getSender().getRole())
                .build();
    }

    public static ReadNotification fromAstraRow(Row row) {
        if (row == null) {
            return null;
        }
        String typeValue = row.getString("type");
        String role = row.getString("sender_role");
        return ReadNotification.builder()
                .profileId(row.getUuid("profile_id"))
                .createdAt(row.getInstant("created_at"))
                .type(typeValue != null ? NotificationType.valueOf(typeValue) : null)
                .readAt(row.getInstant("read_at"))
                .body(row.getString("body"))
                .senderId(row.getUuid("sender_id"))
                .senderUsername(row.getString("sender_username"))
                .senderImage192(row.getUuid("sender_image_192"))
                .senderGender(row.getList("sender_gender", String.class))
                .senderRole(role != null ? Role.PomeranianRole.valueOf(role) : null)
                .build();
    }
}

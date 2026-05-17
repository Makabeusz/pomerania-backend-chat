package com.sojka.pomeranian.notification.util;

import com.datastax.oss.driver.api.core.cql.Row;
import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.lib.dto.NotificationType;
import com.sojka.pomeranian.lib.dto.UserData;
import com.sojka.pomeranian.lib.util.JsonUtils;
import com.sojka.pomeranian.notification.model.NotificationModel;
import com.sojka.pomeranian.security.model.Role;
import lombok.extern.slf4j.Slf4j;

import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toInstant;

@Slf4j
public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static Notification<Object> toDto(NotificationModel notification) {
        if (notification == null) {
            return null;
        }

        Object body = null;
        if (notification.getBody() != null && !notification.getBody().isEmpty()) {
            try {
                body = JsonUtils.readMap(notification.getBody());
            } catch (Exception e) {
                log.error("Notification body failed to map: message={}, rawBody={}", e.getMessage(), notification.getBody());
            }
        }

        return Notification.builder()
                .profileId(notification.getProfileId())
                .createdAt(toDateString(notification.getCreatedAt()))
                .type(notification.getType())
                .body(body)
                .sender(UserData.builder()
                        .id(notification.getSenderId())
                        .username(notification.getSenderUsername())
                        .image192(notification.getSenderImage192())
                        .gender(notification.getSenderGender())
                        .role(notification.getSenderRole())
                        .build())
                .build();
    }

    public static NotificationModel toDomain(Notification<Object> notification) {
        if (notification == null) {
            return null;
        }

        return NotificationModel.builder()
                .profileId(notification.getProfileId())
                .createdAt(toInstant(notification.getCreatedAt()))
                .type(notification.getType())
                .body(notification.getBody() == null ? null : JsonUtils.writeToString(notification.getBody()).trim())
                .senderId(notification.getSender().getId())
                .senderUsername(notification.getSender().getUsername())
                .senderImage192(notification.getSender().getImage192())
                .senderGender(notification.getSender().getGender())
                .senderRole(notification.getSender().getRole())
                .build();
    }

    public static NotificationModel fromAstraRow(Row row) {
        if (row == null) {
            return null;
        }
        String typeValue = row.getString("type");
        String role = row.getString("sender_role");
        return NotificationModel.builder()
                .profileId(row.getUuid("profile_id"))
                .createdAt(row.getInstant("created_at"))
                .type(typeValue != null ? NotificationType.valueOf(typeValue) : null)
                .body(row.getString("body"))
                .senderId(row.getUuid("sender_id"))
                .senderUsername(row.getString("sender_username"))
                .senderImage192(row.getUuid("sender_image_192"))
                .senderGender(row.getList("sender_gender", String.class))
                .senderRole(role != null ? Role.PomeranianRole.valueOf(role) : null)
                .build();
    }
}

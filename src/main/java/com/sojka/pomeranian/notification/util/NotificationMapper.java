package com.sojka.pomeranian.notification.util;

import com.datastax.oss.driver.api.core.cql.Row;
import com.sojka.pomeranian.chat.dto.NotificationDto;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.notification.model.Notification;

import java.util.Optional;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationDto toDto(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationDto.builder()
                .profileId(notification.getProfileId())
                .createdAt(CommonUtils.formatToDateString(notification.getCreatedAt()))
                .type(Optional.ofNullable(notification.getType()).map(Notification.Type::name).orElse(null))
                .readAt(CommonUtils.formatToDateString(notification.getReadAt()))
                .relatedId(notification.getRelatedId())
                .content(notification.getContent())
                .metadata(notification.getMetadata())
                .build();
    }

    public static Notification toDomain(NotificationDto notification) {
        if (notification == null) {
            return null;
        }
        return Notification.builder()
                .profileId(notification.getProfileId())
                .createdAt(CommonUtils.formatToInstant(notification.getCreatedAt()))
                .type(Notification.Type.valueOf(notification.getType()))
                .readAt(CommonUtils.formatToInstant(notification.getReadAt()))
                .relatedId(notification.getRelatedId())
                .content(notification.getContent())
                .metadata(notification.getMetadata())
                .build();
    }

    public static Notification fromAstraRow(Row row) {
        if (row == null) {
            return null;
        }
        String typeValue = row.getString("type");
        Notification.Type type = typeValue != null ? Notification.Type.valueOf(typeValue) : null;
        return Notification.builder()
                .profileId(row.getString("profile_id"))
                .createdAt(row.getInstant("created_at"))
                .type(type)
                .readAt(row.getInstant("read_at"))
                .relatedId(row.getString("related_id"))
                .content(row.getString("content"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .build();
    }
}

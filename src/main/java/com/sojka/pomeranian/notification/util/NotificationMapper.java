package com.sojka.pomeranian.notification.util;

import com.datastax.oss.driver.api.core.cql.Row;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.notification.dto.NotificationDto;
import com.sojka.pomeranian.notification.model.Notification;
import com.sojka.pomeranian.notification.model.NotificationType;

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
                .type(Optional.ofNullable(notification.getType()).map(NotificationType::name).orElse(null))
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
                .type(NotificationType.valueOf(notification.getType()))
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
        NotificationType type = typeValue != null ? NotificationType.valueOf(typeValue) : null;
        return Notification.builder()
                .profileId(row.getString("profile_id"))
                .createdAt(row.getInstant("created_at"))
                .type(type)
                .relatedId(row.getString("related_id"))
                .content(row.getString("content"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .build();
    }
}

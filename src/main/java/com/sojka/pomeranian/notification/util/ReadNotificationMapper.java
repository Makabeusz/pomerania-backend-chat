package com.sojka.pomeranian.notification.util;

import com.datastax.oss.driver.api.core.cql.Row;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.notification.dto.NotificationDto;
import com.sojka.pomeranian.notification.model.NotificationType;
import com.sojka.pomeranian.notification.model.ReadNotification;

import java.time.Instant;
import java.util.Optional;

public final class ReadNotificationMapper {

    private ReadNotificationMapper() {
    }

    public static NotificationDto toDto(ReadNotification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationDto.builder()
                .profileId(notification.getProfileId())
                .createdAt(CommonUtils.formatToDateString(notification.getCreatedAt()))
                .type(Optional.ofNullable(notification.getType()).map(NotificationType::name).orElse(null))
                .readAt(CommonUtils.formatToDateString(notification.getReadAt()))
                .relatedId(notification.getRelatedId())
                .content(notification.getContent())
                .metadata(notification.getMetadata())
                .build();
    }

    public static ReadNotification toReadNotificationDomain(NotificationDto notification, Instant readAt) {
        if (notification == null) {
            return null;
        }
        return ReadNotification.builder()
                .profileId(notification.getProfileId())
                .createdAt(CommonUtils.formatToInstant(notification.getCreatedAt()))
                .type(NotificationType.valueOf(notification.getType()))
                .readAt(readAt)
                .relatedId(notification.getRelatedId())
                .content(notification.getContent())
                .metadata(notification.getMetadata())
                .build();
    }

    public static ReadNotification fromAstraRow(Row row) {
        if (row == null) {
            return null;
        }
        String typeValue = row.getString("type");
        NotificationType type = typeValue != null ? NotificationType.valueOf(typeValue) : null;
        return ReadNotification.builder()
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

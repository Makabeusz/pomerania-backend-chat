package com.sojka.pomeranian.notification.util;

import com.datastax.oss.driver.api.core.cql.Row;
import com.sojka.pomeranian.lib.dto.NotificationDto;
import com.sojka.pomeranian.notification.model.ReadNotification;

import java.time.Instant;

import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toInstant;

public final class ReadNotificationMapper {

    private ReadNotificationMapper() {
    }

    public static NotificationDto toDto(ReadNotification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationDto.builder()
                .profileId(notification.getProfileId())
                .createdAt(toDateString(notification.getCreatedAt()))
                .type(notification.getType())
                .readAt(toDateString(notification.getReadAt()))
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
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
                .createdAt(toInstant(notification.getCreatedAt()))
                .type(notification.getType())
                .readAt(readAt)
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .content(notification.getContent())
                .metadata(notification.getMetadata())
                .build();
    }

    public static ReadNotification fromAstraRow(Row row) {
        if (row == null) {
            return null;
        }
        String typeValue = row.getString("type");
        return ReadNotification.builder()
                .profileId(row.getUuid("profile_id"))
                .createdAt(row.getInstant("created_at"))
                .type(typeValue != null ? NotificationDto.Type.valueOf(typeValue) : null)
                .readAt(row.getInstant("read_at"))
                .relatedId(row.getUuid("related_id"))
                .relatedType(row.getString("related_type"))
                .content(row.getString("content"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .build();
    }
}

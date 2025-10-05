package com.sojka.pomeranian.notification.util;

import com.datastax.oss.driver.api.core.cql.Row;
import com.sojka.pomeranian.lib.dto.NotificationDto;
import com.sojka.pomeranian.notification.model.Notification;

import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toInstant;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationDto toDto(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationDto.builder()
                .profileId(notification.getProfileId())
                .createdAt(toDateString(notification.getCreatedAt()))
                .type(notification.getType())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
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
                .createdAt(toInstant(notification.getCreatedAt()))
                .type(notification.getType())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .content(notification.getContent())
                .metadata(notification.getMetadata())
                .build();
    }

    public static Notification fromAstraRow(Row row) {
        if (row == null) {
            return null;
        }
        String typeValue = row.getString("type");
        return Notification.builder()
                .profileId(row.getString("profile_id"))
                .createdAt(row.getInstant("created_at"))
                .type(typeValue != null ? NotificationDto.Type.valueOf(typeValue) : null)
                .relatedId(row.getString("related_id"))
                .relatedType(row.getString("related_type"))
                .content(row.getString("content"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .build();
    }
}

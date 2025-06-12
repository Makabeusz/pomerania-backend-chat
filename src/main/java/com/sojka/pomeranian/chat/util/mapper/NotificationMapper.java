package com.sojka.pomeranian.chat.util.mapper;

import com.datastax.oss.driver.api.core.cql.Row;
import com.sojka.pomeranian.chat.dto.NotificationMessage;
import com.sojka.pomeranian.chat.model.Notification;
import com.sojka.pomeranian.chat.util.CommonUtils;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationMessage toDto(Notification notification) {
        return NotificationMessage.builder()
                .profileId(notification.getProfileId())
                .createdAt(CommonUtils.formatToDateString(notification.getCreatedAt()))
                .senderId(notification.getSenderId())
                .senderUsername(notification.getSenderUsername())
                .content(notification.getContent())
                .build();
    }

    public static Notification fromAstraRow(Row row) {
        return Notification.builder()
                .profileId(row.getString("profile_id"))
                .createdAt(row.getInstant("created_at"))
                .senderId(row.getString("sender_id"))
                .senderUsername(row.getString("sender_username"))
                .content(row.getString("content"))
                .build();
    }
}

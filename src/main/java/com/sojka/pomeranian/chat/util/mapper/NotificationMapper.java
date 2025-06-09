package com.sojka.pomeranian.chat.util.mapper;

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
}

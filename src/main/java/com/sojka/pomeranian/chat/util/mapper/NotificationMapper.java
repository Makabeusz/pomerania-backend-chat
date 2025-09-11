package com.sojka.pomeranian.chat.util.mapper;

import com.sojka.pomeranian.chat.dto.MessageNotificationDto;
import com.sojka.pomeranian.chat.dto.NotificationHeader;
import com.sojka.pomeranian.chat.dto.NotificationHeaderDto;
import com.sojka.pomeranian.chat.model.MessageNotification;

import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static MessageNotificationDto toDto(MessageNotification notification) {
        return MessageNotificationDto.builder()
                .profileId(notification.getId().getProfileId())
                .createdAt(toDateString(notification.getId().getCreatedAt()))
                .senderId(notification.getId().getSenderId())
                .senderUsername(notification.getSenderUsername())
                .content(notification.getContent())
                .build();
    }

    public static NotificationHeaderDto toDto(NotificationHeader notification) {
        return NotificationHeaderDto.builder()
                .profileId(notification.getProfileId())
                .createdAt(toDateString(notification.getCreatedAt().toLocalDateTime()))
                .senderId(notification.getSenderId())
                .senderUsername(notification.getSenderUsername())
                .content(notification.getContent())
                .count(notification.getCount() >= Integer.MAX_VALUE ? Integer.MAX_VALUE : notification.getCount().intValue())
                .build();
    }

}

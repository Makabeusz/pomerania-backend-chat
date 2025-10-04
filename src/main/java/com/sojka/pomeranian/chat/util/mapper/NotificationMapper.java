package com.sojka.pomeranian.chat.util.mapper;

import com.sojka.pomeranian.chat.dto.NotificationHeader;
import com.sojka.pomeranian.chat.model.MessageNotification;
import com.sojka.pomeranian.notification.dto.NotificationDto;

import java.util.HashMap;
import java.util.Map;

import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationDto toDto(MessageNotification notification) {
        return NotificationDto.builder()
                .profileId(notification.getId().getProfileId())
                .createdAt(toDateString(notification.getId().getCreatedAt()))
                .content(notification.getContent())
                .metadata(new HashMap<>(Map.of(
                        "senderId", notification.getId().getSenderId(),
                        "senderUsername", notification.getSenderUsername()
                )))
                .build();
    }

    public static NotificationDto toDto(NotificationHeader notification) {
        return NotificationDto.builder()
                .profileId(notification.getProfileId())
                .createdAt(toDateString(notification.getCreatedAt().toLocalDateTime()))
                .content(notification.getContent())
                .metadata(new HashMap<>(Map.of(
                        "senderId", notification.getSenderId(),
                        "senderUsername", notification.getSenderUsername(),
                        "count", notification.getCount() >= Integer.MAX_VALUE
                                ? Integer.MAX_VALUE + ""
                                : notification.getCount().toString()
                )))
                .build();
    }

}

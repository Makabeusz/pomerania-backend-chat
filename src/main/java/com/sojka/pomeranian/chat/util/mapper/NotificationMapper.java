package com.sojka.pomeranian.chat.util.mapper;

import com.sojka.pomeranian.chat.dto.NotificationDto;
import com.sojka.pomeranian.chat.dto.NotificationHeader;
import com.sojka.pomeranian.chat.dto.NotificationHeaderDto;
import com.sojka.pomeranian.chat.model.Notification;
import com.sojka.pomeranian.chat.util.CommonUtils;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationDto toDto(Notification notification) {
        return NotificationDto.builder()
                .profileId(notification.getId().getProfileId())
                .createdAt(CommonUtils.formatToDateString(notification.getId().getCreatedAt()))
                .senderId(notification.getId().getSenderId())
                .senderUsername(notification.getSenderUsername())
                .content(notification.getContent())
                .build();
    }

    public static NotificationHeaderDto toDto(NotificationHeader notification) {
        return NotificationHeaderDto.builder()
                .profileId(notification.getProfileId())
                .createdAt(CommonUtils.formatToDateString(notification.getCreatedAt().toLocalDateTime()))
                .senderId(notification.getSenderId())
                .senderUsername(notification.getSenderUsername())
                .content(notification.getContent())
                .count(notification.getCount() >= Integer.MAX_VALUE ? Integer.MAX_VALUE : notification.getCount().intValue())
                .build();
    }

}

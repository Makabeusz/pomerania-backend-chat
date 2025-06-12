package com.sojka.pomeranian.chat.util.mapper;

import com.sojka.pomeranian.chat.dto.NotificationDto;
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

//    public static Notification fromAstraRow(Row row) {
//        return Notification.builder()
//                .profileId(row.getString("profile_id"))
//                .createdAt(row.getInstant("created_at"))
//                .senderId(row.getString("sender_id"))
//                .senderUsername(row.getString("sender_username"))
//                .content(row.getString("content"))
//                .build();
//    }
}

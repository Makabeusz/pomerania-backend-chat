package com.sojka.pomeranian.chat.util.mapper;

import com.sojka.pomeranian.chat.dto.NotificationHeader;
import com.sojka.pomeranian.chat.model.MessageNotification;
import com.sojka.pomeranian.lib.dto.CommentStompRequest;
import com.sojka.pomeranian.lib.dto.NotificationDto;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.sojka.pomeranian.lib.util.CommonUtils.getNameOrNull;
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

    public static NotificationDto toDto(CommentStompRequest request) {
        var metadata = new HashMap<String, String>();
        Optional.ofNullable(request.getProfileId()).ifPresent(id -> metadata.put("senderId", id));
        Optional.ofNullable(request.getUsername()).ifPresent(username -> metadata.put("senderUsername", username));
        Optional.ofNullable(request.getRelatedLocationId()).ifPresent(id -> metadata.put("relatedLocationId", id));

        return NotificationDto.builder()
                .profileId(request.getRelatedProfileId())
                .createdAt(request.getCreatedAt())
                .type(NotificationDto.Type.COMMENT)
//                .readAt()
                .content(request.getContent())
                .relatedId(request.getRelatedId())
                .relatedType(getNameOrNull(request.getRelatedType()))
                .metadata(metadata)
                .build();
    }

}

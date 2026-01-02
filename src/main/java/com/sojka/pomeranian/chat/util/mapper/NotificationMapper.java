package com.sojka.pomeranian.chat.util.mapper;

import com.sojka.pomeranian.chat.dto.ConversationDto;
import com.sojka.pomeranian.chat.repository.projection.ConversationProjection;
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

    public static NotificationDto toDto(ConversationDto conversation) {
        return NotificationDto.builder()
                .createdAt(toDateString(conversation.getLastMessageAt()))
                .content(conversation.getContent())
                .metadata(new HashMap<>(Map.of(
                        "senderId", conversation.getRecipient().id() + "",
                        "senderUsername", conversation.getRecipient().username() + "", // fix null
                        "senderImage192", conversation.getRecipient().image192() + ""
                )))
                .build();
    }

    public static NotificationDto toDto(ConversationProjection projection) {
        return NotificationDto.builder()
                .profileId(projection.getRecipientId())
                .createdAt(toDateString(projection.getLastMessageAt()))
                .content(projection.getContent())
                .relatedType(projection.getContentType())
                .metadata(new HashMap<>(Map.of(
                        "senderImage192", projection.getRecipientImage192() + "",
                        "senderUsername", projection.getRecipientUsername() + "", // fix null
                        "unreadCount", projection.getUnreadCount() + ""
                )))
                .build();
    }

    public static NotificationDto toDto(CommentStompRequest request) {
        var metadata = new HashMap<String, String>();
        Optional.ofNullable(request.getImage192()).ifPresent(image192 -> metadata.put("image192", image192));
        Optional.ofNullable(request.getProfileId()).ifPresent(id -> metadata.put("senderId", id + ""));
        Optional.ofNullable(request.getUsername()).ifPresent(username -> metadata.put("senderUsername", username));
        Optional.ofNullable(request.getRelatedLocationId()).ifPresent(idOrUsername -> metadata.put("relatedLocationId", idOrUsername));

        return NotificationDto.builder()
                .profileId(request.getRelatedProfileId())
                .createdAt(request.getCreatedAt())
                .type(NotificationDto.Type.COMMENT)
                .content(request.getContent())
                .relatedId(request.getRelatedId())
                .relatedType(getNameOrNull(request.getRelatedType()))
                .metadata(metadata)
                .build();
    }

}

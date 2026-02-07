package com.sojka.pomeranian.chat.util.mapper;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.repository.projection.ConversationProjection;
import com.sojka.pomeranian.lib.dto.CommentStompRequest;
import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.lib.dto.NotificationType;
import com.sojka.pomeranian.lib.dto.UserData;
import com.sojka.pomeranian.lib.util.JsonUtils;
import com.sojka.pomeranian.security.model.Role;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.sojka.pomeranian.lib.util.CommonUtils.getNameOrNull;
import static com.sojka.pomeranian.lib.util.CommonUtils.sliceDescription;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static Notification toNotification(ChatMessage message, String createdAt) {
        return Notification.builder()
                .createdAt(createdAt)
                .sender(message.getSender())
                .type(NotificationType.MESSAGE)
                .body(createMessageBody(message.getContent(), Optional.ofNullable(message.getResource()).orElse(new ChatMessage.Resource()).getType() + ""))
                .build();
    }

    public static Notification toNotification(ConversationProjection projection) {
        return Notification.builder()
                .sender(UserData.builder()
                        .id(projection.getRecipientId())
                        .image192(projection.getRecipientImage192())
                        .username(projection.getRecipientUsername())
                        .gender(projection.getGender())
                        .role(projection.getRoleId() == null ? null : Role.PomeranianRole.fromOrdinal(projection.getRoleId()))
                        .build())
                .createdAt(toDateString(projection.getLastMessageAt()))
                .body(createMessageBody(projection.getContent(), projection.getContentType(), projection.getUnreadCount()))
                .build();
    }

    public static Notification toNotification(CommentStompRequest request) {
        var body = new HashMap<String, String>();
        Optional.ofNullable(request.getRelatedLocationId()).ifPresent(idOrUsername -> body.put("relatedLocationId", idOrUsername));
        body.put("content", request.getContent());
        body.put("relatedId", request.getRelatedId() + "");
        body.put("relatedType", getNameOrNull(request.getRelatedType()));

        return Notification.builder()
                .profileId(request.getRelatedProfileId())
                .sender(UserData.builder()
                        .id(request.getProfileId())
                        .image192(Optional.ofNullable(request.getImage192()).map(UUID::fromString).orElse(null))
                        .username(request.getUsername())
//                        .gender()
//                        .role()
                        .build())
                .createdAt(request.getCreatedAt())
                .type(NotificationType.COMMENT)
                .body(JsonUtils.writeToString(body))
                .build();
    }

    private static String createMessageBody(String content, String type) {
        return createMessageBody(content, type, null);
    }

    private static String createMessageBody(String content, String type, Integer unreadCount) {
        Map<String, Object> body = Map.of(
                "content", sliceDescription(content, 200),
                "type", type,
                "unreadCount", (unreadCount == null ? 0 : unreadCount) + ""
        );
        return JsonUtils.writeToString(body);
    }

}

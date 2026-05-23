package com.sojka.pomeranian.chat.util.mapper;

import com.sojka.pomeranian.chat.repository.projection.ConversationProjection;
import com.sojka.pomeranian.lib.dto.CommentStompRequest;
import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.lib.dto.NotificationType;
import com.sojka.pomeranian.lib.dto.UserData;
import com.sojka.pomeranian.security.model.Role;

import java.util.HashMap;
import java.util.Map;

import static com.sojka.pomeranian.lib.util.CommonUtils.sliceDescription;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static Notification<Object> toNotification(ConversationProjection projection) {
        return Notification.builder()
                .createdAt(toDateString(projection.getLastMessageAt()))
                .sender(UserData.builder()
                        .id(projection.getRecipientId())
                        .role(projection.getRoleId() == null ? null : Role.PomeranianRole.fromOrdinal(projection.getRoleId()))
                        .build())
                .type(NotificationType.MESSAGE)
                .body(createMessageBody(projection.getContent(), projection.getContentType(), projection.getUnreadCount()))
                .build();
    }

    public static Notification<Object> toNotification(CommentStompRequest request) {
        var body = new HashMap<>(Map.of(
                "id", request.getId(),
                "element", request.getElement(),
                "content", request.getContent()
        ));
        if (request.getCommenterId() != null) {
            body.put("commenter", Map.of(
                    "id", request.getCommenterId(),
                    "name", request.getCommenterName())
            );
        }
        if (request.getUpdatedAt() != null) {
            body.put("updatedAt", request.getUpdatedAt());
        }

        return Notification.builder()
                .profileId(request.getElement().getOwner().getId())
                .sender(UserData.builder().id(request.getSender().getId()).build())
                .createdAt(request.getCreatedAt())
                .type(NotificationType.COMMENT)
                .body(body)
                .build();
    }

    private static Object createMessageBody(String content, String type, Integer unreadCount) {
        return Map.of(
                "content", sliceDescription(content, 200),
                "type", type,
                "unreadCount", unreadCount
        );
    }

}

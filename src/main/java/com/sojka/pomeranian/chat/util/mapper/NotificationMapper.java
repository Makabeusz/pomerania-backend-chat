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

import static com.sojka.pomeranian.lib.util.CommonUtils.sliceDescription;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static Notification<Map<String, Object>> toNotification(ChatMessage message, String createdAt) {
        var notification = new Notification<Map<String, Object>>();
        notification.setCreatedAt(createdAt);
        notification.setSender(message.getSender());
        notification.setType(NotificationType.MESSAGE);
        notification.setBody(createMessageBody(message.getContent(), Optional.ofNullable(message.getResource()).orElse(new ChatMessage.Resource()).getType() + ""));
        return notification;
    }

    public static Notification<Map<String, Object>> toNotification(ConversationProjection projection) {
        var notification = new Notification<Map<String, Object>>();
        notification.setCreatedAt(toDateString(projection.getLastMessageAt()));
        notification.setSender(UserData.builder()
                .id(projection.getRecipientId())
                .image192(projection.getRecipientImage192())
                .username(projection.getRecipientUsername())
                .gender(projection.getGender())
                .role(projection.getRoleId() == null ? null : Role.PomeranianRole.fromOrdinal(projection.getRoleId()))
                .build());
//        notification.setType(NotificationType.MESSAGE);
        notification.setBody(createMessageBody(projection.getContent(), projection.getContentType(), projection.getUnreadCount()));
        return notification;
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
                .sender(request.getSender())
                .createdAt(request.getCreatedAt())
                .type(NotificationType.COMMENT)
                .body(JsonUtils.writeToString(body))
                .build();
    }

    private static Map<String, Object> createMessageBody(String content, String type) {
        return createMessageBody(content, type, null);
    }

    private static Map<String, Object> createMessageBody(String content, String type, Integer unreadCount) {
        return Map.of(
                "content", sliceDescription(content, 200),
                "type", type,
                "unreadCount", (unreadCount == null ? 0 : unreadCount) + ""
        );
    }

}

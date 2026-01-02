package com.sojka.pomeranian.chat.model;

import com.sojka.pomeranian.lib.dto.ConversationFlag;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Table(name = "conversations")
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @EmbeddedId
    private Id id;

    @Column(name = "flag", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConversationFlag flag;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    @Column(name = "content")
    private String content;

    @Column(name = "content_type")
    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    @Column(name = "unread_count", nullable = false)
    private Integer unreadCount;

    @Column(name = "is_last_message_from_user", nullable = false)
    private Boolean isLastMessageFromUser;

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Id implements Serializable {

        @Column(name = "user_id", nullable = false, updatable = false)
        private UUID userId;

        @Column(name = "recipient_id", nullable = false, updatable = false)
        private UUID recipientId;
    }

    public enum ContentType {
        MESSAGE, IMAGE, VIDEO, MESSAGE_IMAGE, MESSAGE_VIDEO;

        // TODO: move ResourceType to lib from main and use here
        public static ContentType getTypeByMessageData(Message message) {
            if (StringUtils.hasText(message.getContent()) && message.getResourceId() == null) {
                return MESSAGE;
            } else if (StringUtils.hasText(message.getContent()) && message.getResourceId() != null) {
                if ("PHOTO".equals(message.getResourceType())) {
                    return MESSAGE_IMAGE;
                } else if ("VIDEO".equals(message.getResourceType())) {
                    return MESSAGE_VIDEO;
                }
            } else if ("PHOTO".equals(message.getResourceType())) {
                return IMAGE;
            } else if ("VIDEO".equals(message.getResourceType())) {
                return VIDEO;
            }
            return null;
        }
    }
}

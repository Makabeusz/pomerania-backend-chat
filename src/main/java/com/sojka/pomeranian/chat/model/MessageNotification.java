package com.sojka.pomeranian.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageNotification {

    @EmbeddedId
    Id id;

    @Column(name = "sender_username", nullable = false)
    private String senderUsername;

    @Column(name = "sender_image_192", nullable = false)
    private UUID senderImage192;

    @Column(name = "content", nullable = false)
    private String content;

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Id implements Serializable {

        @Column(name = "profile_id", nullable = false, updatable = false)
        private UUID profileId;

        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "sender_id", nullable = false, updatable = false)
        private UUID senderId;
    }

}

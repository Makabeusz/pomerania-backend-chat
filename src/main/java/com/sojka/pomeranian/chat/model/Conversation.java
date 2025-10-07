package com.sojka.pomeranian.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @EmbeddedId
    private Id id;

    @Column(name = "starred")
    private Boolean starred;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Id implements Serializable {

        @Column(name = "user_id", nullable = false, updatable = false)
        private String userId;

        @Column(name = "recipient_id", nullable = false, updatable = false)
        private String recipientId;
    }
}

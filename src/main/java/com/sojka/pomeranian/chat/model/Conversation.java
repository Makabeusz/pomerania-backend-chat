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
    Id id;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    @Embeddable
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Id implements Serializable {

        @Column(name = "user_id", nullable = false, updatable = false)
        private String userId;

        @Column(name = "room_id", nullable = false, updatable = false)
        private String roomId;
    }
}

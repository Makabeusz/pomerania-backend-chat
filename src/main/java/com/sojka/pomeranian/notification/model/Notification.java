package com.sojka.pomeranian.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private String profileId;
    private Instant createdAt;
    private Type type;
    private Instant readAt;
    private String relatedId;
    private String content;
    private Map<String, String> metadata;

    public enum Type {
        FRIEND_REQUEST
    }
}

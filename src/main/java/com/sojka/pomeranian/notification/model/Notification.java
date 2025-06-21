package com.sojka.pomeranian.notification.model;

import com.sojka.pomeranian.chat.dto.NotificationType;
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
    private NotificationType type;
    private String relatedId;
    private String content;
    private Map<String, String> metadata;

}

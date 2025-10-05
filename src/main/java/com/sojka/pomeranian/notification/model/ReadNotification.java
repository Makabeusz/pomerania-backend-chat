package com.sojka.pomeranian.notification.model;

import com.sojka.pomeranian.lib.dto.NotificationDto;
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
public class ReadNotification {

    private String profileId;
    private Instant createdAt;
    private NotificationDto.Type type;
    private Instant readAt;
    private String relatedId;
    private String relatedType;
    private String content;
    private Map<String, String> metadata;
}

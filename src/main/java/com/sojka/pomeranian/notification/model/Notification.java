package com.sojka.pomeranian.notification.model;

import com.sojka.pomeranian.lib.dto.NotificationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private UUID profileId;
    private Instant createdAt;
    private NotificationDto.Type type;
    private UUID relatedId;
    private String relatedType;
    private String content;
    private Map<String, String> metadata;

}

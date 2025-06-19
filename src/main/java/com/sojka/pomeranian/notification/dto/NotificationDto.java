package com.sojka.pomeranian.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String profileId;
    private String createdAt;
    private String type;
    private String relatedId;
    private String content;
    private Map<String, String> metadata;
}

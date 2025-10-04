package com.sojka.pomeranian.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

// TODO: duplicated with main
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String profileId;
    private String createdAt;
    private String type;
    private String readAt;
    private String relatedId;
    private String content;
    private Map<String, String> metadata;

    // TODO: this is missing from main, add it!
    public String addMetadata(String key, String value) {
        return metadata.put(key, value);
    }
}

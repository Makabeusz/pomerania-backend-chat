package com.sojka.pomeranian.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * TODO: senderId and senderUsername can be in metadata
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageNotificationDto {

    private String profileId;
    private String createdAt;
    private String type;
    private String readAt;
    private String relatedId;
    private String content;
    private String senderId;
    private String senderUsername;
    private Map<String, String> metadata;

}

package com.sojka.pomeranian.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private String roomId;
    private String createdAt;
    private String messageId;
    private String profileId;
    private String username;
    private String content;
    private String type;
    private String resourceId;
    private String threadId;
    private String editedAt;
    private String deletedAt;
    private boolean pinned;
    private Map<String, String> metadata;
}

package com.sojka.pomeranian.chat.model;

import java.time.Instant;
import java.util.Map;

public class Message {

    private String roomId;
    private Instant createdAt;
    private String messageId;
    private String profileId;
    private String username;
    private String content;
    private String messageType;
    private String resourceId;
    private String threadId;
    private String editedAt;
    private String deletedAt;
    private Boolean pinned;
    private Map<String, String> metadata;
}

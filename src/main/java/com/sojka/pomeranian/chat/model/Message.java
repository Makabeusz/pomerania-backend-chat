package com.sojka.pomeranian.chat.model;

import jakarta.validation.constraints.NotBlank;
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
public class Message {

    private String roomId;
    private Instant createdAt;
    private UUID profileId;
    @NotBlank
    private String username;
    private UUID recipientProfileId;
    private String recipientUsername;
    // @NotBlank
    private String content;
    private UUID resourceId;
    private String resourceType;
    private UUID threadId;
    private String editedAt;
    private String deletedAt;
    private Boolean pinned;
    private Instant readAt;
    private Map<String, String> metadata;
}

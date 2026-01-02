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
    private String content;
    private UUID resourceId;
    // TODO: move ResourceType to lib from main and use here
    private String resourceType;
    private String editedAt;
    private Instant readAt;
    private Map<String, String> metadata;
}

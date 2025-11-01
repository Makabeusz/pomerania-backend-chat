package com.sojka.pomeranian.chat.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The extended message to supplement {@link ChatMessage} with database details and share with frontend.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ChatMessagePersisted extends ChatMessage {

    private String roomId;
    private String createdAt;
    private UUID resourceId;
    private String resourceType;
    private UUID threadId;
    private String editedAt;
    private String deletedAt;
    private Boolean pinned;
    private String readAt;
    private Map<String, String> metadata;

    @Builder
    public ChatMessagePersisted(String content, ChatUser sender, ChatUser recipient,
                                String roomId, String createdAt, UUID resourceId, String resourceType, UUID threadId, String editedAt,
                                String deletedAt, Boolean pinned, Map<String, String> metadata, String readAt) {
        super(
                content, (resourceId != null && resourceType != null) ? new Resource(resourceId, resourceType) : null,
                sender, recipient
        );
        this.roomId = roomId;
        this.createdAt = createdAt;
        this.resourceId = resourceId;
        this.threadId = threadId;
        this.editedAt = editedAt;
        this.deletedAt = deletedAt;
        this.pinned = pinned;
        this.readAt = readAt;
        this.metadata = metadata;
    }

    public String addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }

        return this.metadata.put(key, value);
    }
}

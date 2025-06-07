package com.sojka.pomeranian.chat.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * The extended message to supplement {@link ChatMessage} with database details and share with frontend.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ChatMessagePersisted extends ChatMessage {

    private String roomId;
    private String createdAt;
    private String resourceId;
    private String threadId;
    private String editedAt;
    private String deletedAt;
    private Boolean pinned;
    private Map<String, String> metadata;

    @Builder
    public ChatMessagePersisted(String content, ChatUser sender, ChatUser recipient,
                                String roomId, String createdAt, String resourceId, String threadId, String editedAt,
                                String deletedAt, Boolean pinned, Map<String, String> metadata) {
        super(content, sender, recipient);
        this.roomId = roomId;
        this.createdAt = createdAt;
        this.resourceId = resourceId;
        this.threadId = threadId;
        this.editedAt = editedAt;
        this.deletedAt = deletedAt;
        this.pinned = pinned;
        this.metadata = metadata;
    }
}

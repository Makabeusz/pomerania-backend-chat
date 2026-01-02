package com.sojka.pomeranian.chat.repository.projection;

import java.time.Instant;
import java.util.UUID;

public interface ConversationProjection {
    UUID getRecipientId();
    String getRecipientUsername();
    UUID getRecipientImage192();
    String getFlag();
    Instant getLastMessageAt();
    String getContent();
    String getContentType();
    Integer getUnreadCount();
}

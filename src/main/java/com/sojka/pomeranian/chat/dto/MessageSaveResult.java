package com.sojka.pomeranian.chat.dto;

public record MessageSaveResult(
        ChatMessagePersisted message,
        ConversationDto notification
) {
}

package com.sojka.pomeranian.chat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * The general chat response DTO.<br>
 * The frontend will map a response objects based on assigned {@link MessageType}.
 * TODO: create some interface for chat types instead of generic here
 *
 * @param <T> The type of the response data.
 */
@Data
@NoArgsConstructor
public class ChatResponse<T> {

    private T data;
    private MessageType type;

    public ChatResponse(@NonNull T data) {
        this.data = data;

        this.type = switch (data.getClass().getSimpleName()) {
            case "ChatMessagePersisted" -> MessageType.CHAT;
            case "ChatRead" -> MessageType.READ;
            default -> throw new RuntimeException("Unrecognized chat response type: " + data.getClass());
        };
    }

    public ChatResponse(@NonNull T data, MessageType type) {
        this.data = data;
        this.type = type;
    }

    public ChatResponse(@NonNull MessageType type) {
        this.type = type;
    }
}

package com.sojka.pomeranian.chat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

/**
 * The general chat response DTO.<br>
 * The frontend will map a response objects based on assigned {@link MessageType}.
 *
 * @param <T> The type of the response data.
 */
@Data
@NoArgsConstructor
public class ChatResponse<T> {

    private List<T> response;
    private MessageType type;

    public ChatResponse(@NonNull List<T> response) {
        if (response.isEmpty()) {
            throw new IllegalArgumentException("Response list cannot be empty");
        }

        this.response = response;

        this.type = switch (response.getFirst().getClass().getSimpleName()) {
            case "ChatMessagePersisted" -> MessageType.CHAT;
            default -> throw new RuntimeException("Unrecognized chat response type: " + response.get(0).getClass());
        };

    }

    public ChatResponse(@NonNull T response) {
        this(List.of(response));
    }
}

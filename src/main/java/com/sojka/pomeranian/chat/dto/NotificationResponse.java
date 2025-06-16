package com.sojka.pomeranian.chat.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * The general notification response DTO.<br>
 * The frontend will map a response objects based on assigned {@link NotificationType}.
 *
 * @param <T> The type of the response data.
 */
@Data
@NoArgsConstructor
public class NotificationResponse<T> {

    private T data;
    private NotificationType type;

    public NotificationResponse(@NonNull T data, NotificationType type) {
        this.data = data;
        this.type = type;
    }

    public NotificationResponse(@NonNull T data, String type) {
        this.data = data;
        this.type = NotificationType.valueOf(type);
    }
}
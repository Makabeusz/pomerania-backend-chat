package com.sojka.pomeranian.chat.dto;

import com.sojka.pomeranian.lib.dto.NotificationDto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * The general notification response DTO.<br>
 * The frontend will map a response objects based on assigned {@link NotificationDto.Type}.
 *
 * @param <T> The type of the response data.
 */
@Data
@NoArgsConstructor
public class NotificationResponse<T> {

    private T data;
    private NotificationDto.Type type;

    public NotificationResponse(@NonNull T data, NotificationDto.Type type) {
        this.data = data;
        this.type = type;
    }

    public NotificationResponse(@NonNull T data, String type) {
        this.data = data;
        this.type = NotificationDto.Type.valueOf(type);
    }
}
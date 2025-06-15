package com.sojka.pomeranian.chat.dto;

import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.model.Notification;

public record MessageSaveResult(
        Message message,
        Notification notification
) {
}

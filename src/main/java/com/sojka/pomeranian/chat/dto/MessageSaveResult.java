package com.sojka.pomeranian.chat.dto;

import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.model.MessageNotification;

public record MessageSaveResult(
        Message message,
        MessageNotification notification
) {
}

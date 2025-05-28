package com.sojka.pomeranian.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessage {

    private String content;
    private ChatUser sender;
    private ChatUser recipient;
    private MessageType type;
}

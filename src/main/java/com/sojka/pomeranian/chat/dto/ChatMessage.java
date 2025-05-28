package com.sojka.pomeranian.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessage {

    private String content;
    private String sender; // Sender's profileId
    private String senderUsername; // Sender's display name
    private String recipientId; // Recipient's profileId
    private MessageType type;
}

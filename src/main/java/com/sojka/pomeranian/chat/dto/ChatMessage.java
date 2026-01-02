package com.sojka.pomeranian.chat.dto;

import com.sojka.pomeranian.lib.dto.ChatUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * The message with minimum informations received from frontend.
 */
@Data
@Builder(builderMethodName = "basicBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private String content;
    private Resource resource;
    private ChatUser sender;
    private ChatUser recipient;

    public ChatUser getSender() {
        return sender == null ? ChatUser.getEmpty() : sender;
    }

    public ChatUser getRecipient() {
        return recipient == null ? ChatUser.getEmpty() : recipient;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resource {
        private UUID id;
        private String type;
    }
}

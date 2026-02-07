package com.sojka.pomeranian.chat.dto;

import com.sojka.pomeranian.lib.dto.UserData;
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
    private UserData sender;
    private UserData recipient;

    public UserData getSender() {
        return sender == null ? UserData.getEmpty() : sender;
    }

    public UserData getRecipient() {
        return recipient == null ? UserData.getEmpty() : recipient;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resource {
        private UUID id;
        private String type;
        private UUID thumbnailId;
    }
}

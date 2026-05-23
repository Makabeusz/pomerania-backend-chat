package com.sojka.pomeranian.chat.dto;

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
    private UserId sender; // ID to be resolved from auth
    private UserId recipient; // only ID

    public UserId getSender() {
        return sender == null ? new UserId() : sender;
    }

    public UserId getRecipient() {
        return recipient == null ? new UserId() : recipient;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resource {
        private UUID id;
        private String type;
        private UUID thumbnailId;
        private Integer height;
        private Integer width;
    }
}

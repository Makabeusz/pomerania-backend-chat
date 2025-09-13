package com.sojka.pomeranian.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resource {
        private String id;
        private String type;
    }
}

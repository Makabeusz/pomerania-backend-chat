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
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    private String content;
    private ChatUser sender;
    private ChatUser recipient;
}

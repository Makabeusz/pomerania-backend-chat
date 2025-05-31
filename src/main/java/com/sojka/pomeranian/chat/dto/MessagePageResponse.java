
package com.sojka.pomeranian.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MessagePageResponse {

    private List<ChatMessageResponse> messages;
    private String nextPageState;
}

package com.sojka.pomeranian.chat.dto;

import com.sojka.pomeranian.lib.dto.ChatUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    private ChatUser recipient;
    private String flag;
    private Instant lastMessageAt;
    private String content;
    private String contentType;
    private Integer unreadCount;
    private Boolean isLastMessageFromUser;
}

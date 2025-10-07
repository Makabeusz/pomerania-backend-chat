package com.sojka.pomeranian.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    private String userId;
    private String recipientId;
    private Boolean starred;
    private Timestamp lastMessageAt;
    private String image192;
}

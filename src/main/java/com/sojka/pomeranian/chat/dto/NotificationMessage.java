package com.sojka.pomeranian.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage {

    private String profileId;
    private String createdAt;
    private String senderId;
    private String senderUsername;
    private String content;

}

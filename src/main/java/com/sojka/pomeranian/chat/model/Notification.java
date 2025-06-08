package com.sojka.pomeranian.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private String profileId;
    private Instant createdAt;
    private String senderId;
    private String senderUsername;
    private String content;

}

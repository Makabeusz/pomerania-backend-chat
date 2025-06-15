package com.sojka.pomeranian.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationHeader {

    private String profileId;
    private Timestamp createdAt;
    private String senderId;
    private String senderUsername;
    private String content;
    private Long count;

}

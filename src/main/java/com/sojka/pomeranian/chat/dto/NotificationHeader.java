package com.sojka.pomeranian.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationHeader {

    private UUID profileId;
    private Timestamp createdAt;
    private UUID senderId;
    private String senderUsername;
    private String content;
    private Long count;

}

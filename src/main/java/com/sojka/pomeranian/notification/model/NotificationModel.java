package com.sojka.pomeranian.notification.model;

import com.sojka.pomeranian.lib.dto.NotificationType;
import com.sojka.pomeranian.security.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationModel {

    private UUID profileId;
    private Instant createdAt;
    private NotificationType type;
    private String body;

    // map to UserData sender
    private UUID senderId;
    private String senderUsername;
    private UUID senderImage192;
    private List<String> senderGender;
    private Role.PomeranianRole senderRole;

}

package com.sojka.pomeranian.notification.controller;

import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.notification.service.NotificationService;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @MessageMapping("/notification.read")
    @PreAuthorize("@authx.isLoggedIn(authentication)")
    public void readNotification(
            @Payload Notification.PrimaryKey dto,
            @AuthenticationPrincipal UsernamePasswordAuthenticationToken principal
    ) {
        User user = (User) principal.getPrincipal();
        dto.setProfileId(user.getId());
        var readAt = notificationService.markRead(dto);

        log.debug("Marked notification as read: {} read_at={}", dto, readAt);
    }
}

package com.sojka.pomeranian.notification.controller;

import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.notification.dto.NotificationDto;
import com.sojka.pomeranian.notification.service.NotificationService;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @MessageMapping("/notification.read")
    public void readMessage(@Payload List<NotificationDto> dto,
                            Principal principal) {
        User user = CommonUtils.getAuthUser(principal);

        var readAt = notificationService.markRead(user.getId(), dto);

        log.info("Marked {} notification as read_at={}", dto.size(), readAt);
    }
}

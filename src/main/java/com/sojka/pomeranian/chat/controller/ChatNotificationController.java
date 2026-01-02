package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.lib.dto.NotificationDto;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/api/chat/notification")
@RequiredArgsConstructor
public class ChatNotificationController {

    private final ChatService chatService;

    @GetMapping("/count")
    @PreAuthorize("hasRole('SOFT_BAN')")
    public ResponseEntity<Long> count(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.countNotifications(user.getId()));
    }

    @GetMapping("/roomCount")
    @PreAuthorize("hasRole('SOFT_BAN')")
    public ResponseEntity<Long> roomCount(
            @AuthenticationPrincipal User user,
            @RequestParam String roomId
    ) {
        return ResponseEntity.ok(chatService.getRoomUnreadMessagesCount(user.getId(), roomId));
    }

    @GetMapping
    @PreAuthorize("hasRole('SOFT_BAN')")
    public ResponseEntity<ResultsPage<NotificationDto>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String nextPageState
    ) {
        return ResponseEntity.ok(chatService.getMessageNotifications(user.getId(), nextPageState));
    }

}

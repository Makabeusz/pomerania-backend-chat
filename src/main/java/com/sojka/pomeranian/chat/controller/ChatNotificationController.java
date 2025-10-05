package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.lib.dto.NotificationDto;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @GetMapping("/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> count(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.countNotifications(user.getId()));
    }

    @GetMapping("/roomCount")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Integer> roomCount(@AuthenticationPrincipal User user,
                                             @RequestParam String roomId) {
        return ResponseEntity.ok(chatService.getUnreadMessagesCount(user.getId(), roomId));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ResultsPage<NotificationDto>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String nextPageState) {
        return ResponseEntity.ok(chatService.getMessageNotifications(user.getId(), nextPageState));
    }

    @GetMapping("/headers")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ResultsPage<NotificationDto>> getNotificationHeaders(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String nextPageState) {
        return ResponseEntity.ok(chatService.getMessageNotificationHeaders(user.getId(), nextPageState));
    }

//    @MessageMapping("/notifications.read")
//    public void sendMessage(@Payload String dummy,
//                            Principal principal) {
//        User user = CommonUtils.getAuthUser(principal);
//
//
//        throw new RuntimeException("unimplemented");
//    }


}

package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.MessageNotificationDto;
import com.sojka.pomeranian.chat.dto.NotificationHeaderDto;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    public ResponseEntity<Long> count(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.countNotifications(user.getId()));
    }

    @GetMapping
    public ResponseEntity<ResultsPage<MessageNotificationDto>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String nextPageState) {
        return ResponseEntity.ok(chatService.getMessageNotifications(user.getId(), nextPageState));
    }

    @GetMapping("/headers")
    public ResponseEntity<ResultsPage<NotificationHeaderDto>> getNotificationHeaders(
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

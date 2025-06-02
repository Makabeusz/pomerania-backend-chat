package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.dto.MessagePageResponse;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<MessagePageResponse> getConversation(
            @RequestParam String recipientId,
            @RequestParam(required = false) String nextPageState,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(chatService.getConversation(user.getId(), recipientId, nextPageState));
    }

    @GetMapping("/headers")
    public ResponseEntity<MessagePageResponse> getConversationHeaders(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String nextPageState
    ) {
        return ResponseEntity.ok(chatService.getConversationsHeaders(user.getId(), nextPageState));
    }

}

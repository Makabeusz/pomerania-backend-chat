package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.dto.MessagePageResponse;
import com.sojka.pomeranian.chat.service.MessageService;
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

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<MessagePageResponse> getConversation(
            @RequestParam String currentUserId,
            @RequestParam String recipientId,
            @RequestParam(required = false) String pageState,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(messageService.getConversation(currentUserId, recipientId, pageState));
    }


}

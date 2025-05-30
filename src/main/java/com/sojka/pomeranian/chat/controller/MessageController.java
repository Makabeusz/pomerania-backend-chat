package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.dto.ChatMessageResponse;
import com.sojka.pomeranian.chat.service.MessageService;
import com.sojka.pomeranian.chat.util.MessageMapper;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Flux<ChatMessageResponse> getConversation(
            @RequestParam String currentUserId,
            @RequestParam String recipientId,
            @RequestParam(required = false) String pageState,
            @AuthenticationPrincipal User user
    ) {
        if (!user.getId().equals(currentUserId)) {
            log.error("User ID={} requested other users conversation: userId1={}, userId2={}",
                    user.getId(), currentUserId, recipientId);
            return Flux.error(new SecurityException("Unauthorized access to conversation"));
        }
        
        return messageService.getConversation(currentUserId, recipientId, pageState).map(MessageMapper::toDto);
    }
}

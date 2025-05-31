package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.service.MessageService;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    @MessageMapping("/chat.sendMessage")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage,
                                   @AuthenticationPrincipal User user) {
        System.out.println("------------------");
        System.out.println(chatMessage);
        System.out.println(user);
        System.out.println("------------------");

        messageService.saveMessage(chatMessage);

        messagingTemplate.convertAndSendToUser(chatMessage.getSender().username(), "/queue/private", chatMessage);
        messagingTemplate.convertAndSendToUser(chatMessage.getRecipient().username(), "/queue/private", chatMessage);

        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        // Add username in web socket session
        System.out.println("==================");
        System.out.println(chatMessage);
        System.out.println("==================");
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender().username());
        return chatMessage;
    }
}

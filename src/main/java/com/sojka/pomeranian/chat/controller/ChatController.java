package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.chat.util.MessageMapper;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage,
                            Principal principal) {
        var message = chatService.saveMessage(chatMessage);
        var messageResponse = MessageMapper.toDto(message);

        User user;
        if (principal instanceof UsernamePasswordAuthenticationToken p) {
            user = (User) p.getPrincipal();
        } else {
            throw new SecurityException("User authentication failed: Not found principal");
        }

        if (messageResponse.getType().equals("INITIAL_MESSAGE")) {
            chatService.initConversation(user.getId(), messageResponse.getRoomId());
        }

        messagingTemplate.convertAndSendToUser(chatMessage.getSender().id(), "/queue/private", messageResponse);
        messagingTemplate.convertAndSendToUser(chatMessage.getRecipient().id(), "/queue/private", messageResponse);
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        System.out.println("===============");
        System.out.println(chatMessage);
        System.out.println("===============");

        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender().username());
        return chatMessage;
    }
}

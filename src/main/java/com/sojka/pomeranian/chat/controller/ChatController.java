package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatReadResponse;
import com.sojka.pomeranian.chat.dto.ChatResponse;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessageKeyResponse;
import com.sojka.pomeranian.chat.dto.ReadMessageDto;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.chat.service.SessionTracker;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.MessageMapper;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final SessionTracker sessionTracker;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage,
                            Principal principal) {
        // todo: don't send sender ID from frontend, fetch it with auth
        User user = CommonUtils.getAuthUser(principal);

        boolean online = sessionTracker.isUserOnline(user.getId());
        log.info("sendMessage user online: {}", online);

        var message = chatService.saveMessage(chatMessage, online);
        var messageResponse = MessageMapper.toDto(message);

        messagingTemplate.convertAndSendToUser(messageResponse.getRoomId(), "/queue/private",
                new ChatResponse<>(messageResponse));
    }

    @MessageMapping("/chat.readMessage")
    public void readIndicator(@Payload List<ReadMessageDto> dto,
                              Principal principal) {
        User user = CommonUtils.getAuthUser(principal);
        List<MessageKey> keys = dto.stream()
                .map(m -> new MessageKey(m.roomId(), CommonUtils.formatToInstant(m.createdAt()), user.getId()))
                .toList();

        var readAt = chatService.markRead(keys);

//        var response = new ChatReadResponse(keys.stream()
//                .map(k -> new MessageKey(
//                        k.roomId(), CommonUtils.formatToDateString(k.createdAt()), k.profileId()))
//                .toList(),
//                CommonUtils.formatToDateString(readAt)
//        );

        log.info("readIndicator user online: {}", sessionTracker.isUserOnline(user.getId()));
        messagingTemplate.convertAndSendToUser(dto.getFirst().roomId(), "/queue/private", null);
        if (!sessionTracker.isUserOnline(user.getId())) {
            // TODO: push notification, will implement this later
            log.warn("NO ACTIVE SESSION TO PUBLISH READ INDICATOR: size={}, last_key={}", keys.size(), keys.getLast());
        }
    }

}

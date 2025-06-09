package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatRead;
import com.sojka.pomeranian.chat.dto.ChatResponse;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.NotificationResponse;
import com.sojka.pomeranian.chat.dto.ReadMessageDto;
import com.sojka.pomeranian.chat.dto.StompConnector;
import com.sojka.pomeranian.chat.service.ChatCache;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.chat.service.SessionTracker;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import com.sojka.pomeranian.chat.util.mapper.NotificationMapper;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

import static com.sojka.pomeranian.chat.util.CommonUtils.getRecipientIdFromRoomId;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private static final String DM_DESTINATION = "/queue/private";
    private static final String NOTIFY_DESTINATION = "/queue/private/notification";

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final SessionTracker sessionTracker;
    private final ChatCache cache;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage,
                            Principal principal) {
        // todo: don't send sender ID from frontend, fetch it with auth
        User user = CommonUtils.getAuthUser(principal);

        boolean isOnline = sessionTracker.isUserOnline(chatMessage.getRecipient().id());

        var messageSaveResult = chatService.saveMessage(chatMessage, isOnline);
        var messageResponse = MessageMapper.toDto(messageSaveResult.message());

        // Update both users chat
        messagingTemplate.convertAndSendToUser(messageResponse.getRoomId(), DM_DESTINATION,
                new ChatResponse<>(messageResponse));
        // Publish unread message notification
        if (!isOnline) {
            var notificationDto = NotificationMapper.toDto(messageSaveResult.notification());
            messagingTemplate.convertAndSendToUser(notificationDto.getProfileId(), NOTIFY_DESTINATION,
                    new NotificationResponse<>(notificationDto));
        }
    }

    @MessageMapping("/chat.readMessage")
    public void readMessage(@Payload ReadMessageDto dto,
                            Principal principal) {
        User user = CommonUtils.getAuthUser(principal);
        var recipientId = getRecipientIdFromRoomId(dto.roomId(), user.getId());

        var readAt = chatService.markRead(
                new MessageKey(dto.roomId(), dto.createdAt().stream()
                        .map(CommonUtils::formatToInstant)
                        .toList(), recipientId));

        log.info("readIndicator user online: {}", sessionTracker.isUserOnline(user.getId()));
        messagingTemplate.convertAndSendToUser(dto.roomId(), DM_DESTINATION,
                new ChatResponse<>(new ChatRead(dto.createdAt(), CommonUtils.formatToDateString(readAt))));

    }

    @MessageMapping("/chat.disconnect")
    public void disconnect(@Payload StompConnector connector,
                           Principal principal) {
        removeFromCache(CommonUtils.getAuthUser(principal).getId(), connector.getName());
    }

    @PostMapping
    @RequestMapping("/api/chat/disconnect/{connector}")
    public ResponseEntity<?> disconnectRest(@AuthenticationPrincipal User user,
                                            @PathVariable("connector") StompConnector connector) {
        removeFromCache(user.getId(), connector.getName());

        return ResponseEntity.ok("disconnected");
    }

    void removeFromCache(String userId, String connector) {
        if ("chat".equals(connector)) {
            cache.remove(userId);
            log.info("Disconnected user={}", userId);
        }
    }
}

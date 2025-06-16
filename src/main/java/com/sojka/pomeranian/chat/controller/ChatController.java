package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatRead;
import com.sojka.pomeranian.chat.dto.ChatResponse;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.NotificationResponse;
import com.sojka.pomeranian.chat.dto.NotificationType;
import com.sojka.pomeranian.chat.dto.ReadMessageDto;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.ChatCache;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import com.sojka.pomeranian.chat.util.mapper.NotificationMapper;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

import static com.sojka.pomeranian.chat.util.CommonUtils.getRecipientIdFromRoomId;
import static com.sojka.pomeranian.chat.util.Constants.DM_DESTINATION;
import static com.sojka.pomeranian.chat.util.Constants.NOTIFY_DESTINATION;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final ChatCache cache;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage,
                            Principal principal) {
        // todo: don't send sender ID from frontend, fetch it with auth
        User user = CommonUtils.getAuthUser(principal);
        String roomId = CommonUtils.generateRoomId(chatMessage);

        boolean isOnline = cache.isOnline(chatMessage.getRecipient().id(), new StompSubscription(StompSubscription.Type.CHAT, roomId));
        var messageSaveResult = chatService.saveMessage(chatMessage, roomId, isOnline);
        var messageResponse = MessageMapper.toDto(messageSaveResult.message());

        // Update both users chat
        messagingTemplate.convertAndSendToUser(messageResponse.getRoomId(), DM_DESTINATION,
                new ChatResponse<>(messageResponse));
        // Publish unread message notification
        if (!isOnline) {
            var notificationDto = NotificationMapper.toDto(messageSaveResult.notification());
            messagingTemplate.convertAndSendToUser(notificationDto.getProfileId(), NOTIFY_DESTINATION,
                    new NotificationResponse<>(notificationDto, NotificationType.MESSAGE));
        }
    }

    @MessageMapping("/chat.read")
    public void readMessage(@Payload ReadMessageDto dto,
                            Principal principal) {
        User user = CommonUtils.getAuthUser(principal);
        var recipientId = getRecipientIdFromRoomId(dto.roomId(), user.getId());

        var readAt = chatService.markRead(
                new MessageKey(dto.roomId(), dto.createdAt().stream()
                        .map(CommonUtils::formatToInstant)
                        .toList(), recipientId));

        log.info("Marked as read: {}", dto);
        messagingTemplate.convertAndSendToUser(dto.roomId(), DM_DESTINATION,
                new ChatResponse<>(new ChatRead(dto.createdAt(), CommonUtils.formatToDateString(readAt))));

    }
}

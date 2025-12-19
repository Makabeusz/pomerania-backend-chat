package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatRead;
import com.sojka.pomeranian.chat.dto.ChatResponse;
import com.sojka.pomeranian.chat.dto.ChatUser;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.NotificationResponse;
import com.sojka.pomeranian.chat.dto.ReadMessageDto;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.chat.service.RedisWebSocketService;
import com.sojka.pomeranian.chat.service.cache.ChatCache;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import com.sojka.pomeranian.chat.util.mapper.NotificationMapper;
import com.sojka.pomeranian.lib.dto.NotificationDto;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

import static com.sojka.pomeranian.chat.util.Constants.DM_DESTINATION;
import static com.sojka.pomeranian.chat.util.Constants.NOTIFY_DESTINATION;
import static com.sojka.pomeranian.lib.util.CommonUtils.getAuthUser;
import static com.sojka.pomeranian.lib.util.CommonUtils.getRecipientIdFromRoomId;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final RedisWebSocketService messagingTemplate;
    private final ChatService chatService;
    private final ChatCache cache;

    // TODO: if there is an error here then publish some feedback back to the client
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage,
                            Principal principal) {
        User user = getAuthUser(principal);
        chatMessage.setSender(new ChatUser(user.getId(), user.getUsername(), chatMessage.getSender().image192()));
        String roomId = CommonUtils.generateRoomId(chatMessage);

        boolean isOnline = cache.isOnline(chatMessage.getRecipient().id(), new StompSubscription(StompSubscription.Type.CHAT, roomId));
        var messageSaveResult = chatService.saveMessage(chatMessage, roomId, isOnline);
        var messageResponse = MessageMapper.toDto(messageSaveResult.message());

        // Update both users chat
        messagingTemplate.convertAndSendToUser(messageResponse.getRoomId(), DM_DESTINATION, new ChatResponse<>(messageResponse));
        // Publish unread message notification
        if (!isOnline) {
            var notificationDto = NotificationMapper.toDto(messageSaveResult.notification());

            messagingTemplate.convertAndSendToUser(notificationDto.getProfileId() + "", NOTIFY_DESTINATION,
                    new NotificationResponse<>(notificationDto, NotificationDto.Type.MESSAGE));
        }
    }

    @MessageMapping("/chat.read")
    public void readMessage(@Payload ReadMessageDto dto,
                            Principal principal) {
        User user = getAuthUser(principal);
        var recipientId = getRecipientIdFromRoomId(dto.roomId(), user.getId());

        var readAt = chatService.markRead(
                new MessageKey(dto.roomId(), dto.createdAt().stream()
                        .map(com.sojka.pomeranian.lib.util.DateTimeUtils::toInstant)
                        .toList(), recipientId));

        log.info("Marked as read: {}", dto);
        messagingTemplate.convertAndSendToUser(dto.roomId(), DM_DESTINATION,
                new ChatResponse<>(new ChatRead(dto.createdAt(), toDateString(readAt))));
    }
}

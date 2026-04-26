package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.config.StompRequestAuthenticator;
import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatRead;
import com.sojka.pomeranian.chat.dto.ChatResponse;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.ReadMessageDto;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.chat.service.cache.SessionCache;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.mapper.NotificationMapper;
import com.sojka.pomeranian.lib.dto.UserData;
import com.sojka.pomeranian.lib.util.DateTimeUtils;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import static com.sojka.pomeranian.chat.util.Constants.DM_DESTINATION;
import static com.sojka.pomeranian.chat.util.Constants.NOTIFY_DESTINATION;
import static com.sojka.pomeranian.lib.util.CommonUtils.getRecipientIdFromRoomId;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toDateString;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;
    private final SessionCache cache;
    private final StompRequestAuthenticator authenticator;

    // TODO: if there is an error here then publish some feedback back to the client
    // TODO: it sends back the message even before it finish updating conversations or notifications, so it might still break with properly saved message as well
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage, StompHeaderAccessor headerAccessor) {
        User user = authenticator.getUser(headerAccessor);
        chatMessage.setSender(UserData.builder()
                .id(user.getId())
                .username(user.getUsername())
                .image192(chatMessage.getSender().getImage192())
                .gender(chatMessage.getSender().getGender())
                .role(chatMessage.getSender().getRole())
                .build());
        String roomId = CommonUtils.generateRoomId(chatMessage);

        boolean hasChatOpen = cache.isOnline(
                chatMessage.getRecipient().getId(), new StompSubscription(StompSubscription.Type.CHAT, roomId)
        );
        String createdAt = chatService.processMessage(chatMessage, roomId, hasChatOpen);

        // Publish unread message notification
        if (!hasChatOpen) {
            var notification = NotificationMapper.toNotification(chatMessage, createdAt);
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getRecipient().getId() + "", NOTIFY_DESTINATION, notification
            );
            log.trace("Sent message notification: {}", notification);
        }
    }

    @MessageMapping("/chat.read")
    public void readMessage(
            @Payload ReadMessageDto dto,
            StompHeaderAccessor headerAccessor
    ) {
        User user = authenticator.getUser(headerAccessor);
        var recipientId = getRecipientIdFromRoomId(dto.roomId(), user.getId());

        var readAt = chatService.markRead(
                new MessageKey(dto.roomId(), dto.createdAt().stream()
                        .map(DateTimeUtils::toInstant)
                        .toList(), recipientId));

        log.info("Marked as read: {}", dto);
        messagingTemplate.convertAndSendToUser(dto.roomId(), DM_DESTINATION,
                new ChatResponse<>(new ChatRead(dto.createdAt(), toDateString(readAt))));
    }
}

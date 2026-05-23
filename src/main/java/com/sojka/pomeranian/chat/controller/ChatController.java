package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.config.StompRequestAuthenticator;
import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatRead;
import com.sojka.pomeranian.chat.dto.ChatResetRead;
import com.sojka.pomeranian.chat.dto.ChatResponse;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessageType;
import com.sojka.pomeranian.chat.dto.ReadMessageDto;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.dto.UserId;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.chat.service.cache.SessionCache;
import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.lib.dto.NotificationType;
import com.sojka.pomeranian.lib.util.DateTimeUtils;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.UUID;

import static com.sojka.pomeranian.chat.util.Constants.DM_DESTINATION;
import static com.sojka.pomeranian.chat.util.Constants.NOTIFY_DESTINATION;
import static com.sojka.pomeranian.lib.util.CommonUtils.generateRoomId;
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
        chatMessage.setSender(new UserId(user.getId()));
        String roomId = generateRoomId(user.getId(), chatMessage.getRecipient().getId());

        boolean hasChatOpen = cache.isOnline(
                chatMessage.getRecipient().getId(), new StompSubscription(StompSubscription.Type.CHAT, roomId)
        );
        chatService.processMessage(chatMessage, roomId, hasChatOpen);

        // Publish unread message notification
        if (!hasChatOpen) {
            // temporary disabled any content as it's only purpose is to +1 messages counter on MESSAGE notification type
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getRecipient().getId() + "", NOTIFY_DESTINATION,
                    Notification.builder().type(NotificationType.MESSAGE).build()
            );
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

        log.debug("Marked as read: {}", dto);
        messagingTemplate.convertAndSendToUser(dto.roomId(), DM_DESTINATION,
                new ChatResponse<>(new ChatRead(dto.createdAt(), toDateString(readAt))));
    }

    @MessageMapping("/chat.resetUnread")
    public void resetConversationUnread(
            @Payload UserId dto,
            StompHeaderAccessor headerAccessor
    ) {
        UUID recipientId = dto.getId();
        User user = authenticator.getUser(headerAccessor);
        Long roomCount = chatService.resetConversationUnreadCount(user.getId(), recipientId);

        messagingTemplate.convertAndSendToUser(generateRoomId(recipientId, user.getId()), DM_DESTINATION,
                new ChatResponse<>(new ChatResetRead(user.getId(), roomCount), MessageType.UNREAD_COUNT_UPDATE));
    }
}

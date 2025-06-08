package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatRead;
import com.sojka.pomeranian.chat.dto.ChatResponse;
import com.sojka.pomeranian.chat.dto.MessageKey;
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
            var notification = messageSaveResult.notification();
            messagingTemplate.convertAndSendToUser(notification.getProfileId(), NOTIFY_DESTINATION, notification);
        }
    }

    @MessageMapping("/chat.readMessage")
    public void readIndicator(@Payload ReadMessageDto dto,
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

}

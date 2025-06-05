package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.MessagePage;
import com.sojka.pomeranian.chat.dto.MessagePageResponse;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;

    public Message saveMessage(ChatMessage chatMessage) {
        Message message = new Message();
        message.setRoomId(CommonUtils.generateRoomId(chatMessage));
        message.setCreatedAt(Instant.now());
        message.setMessageId(UUID.randomUUID().toString());
        message.setProfileId(chatMessage.getSender().id());
        message.setUsername(chatMessage.getSender().username());
        message.setRecipientProfileId(chatMessage.getRecipient().id());
        message.setRecipientUsername(chatMessage.getRecipient().username());
        message.setContent(chatMessage.getContent());
        message.setMessageType(chatMessage.getType().toString());
        return messageRepository.save(message);
    }

    public MessagePageResponse getConversation(String userId1, String userId2, String pageState) {
        String roomId = CommonUtils.generateRoomId(userId1, userId2);
        var page = messageRepository.findByRoomId(roomId, pageState, 10);
        return new MessagePageResponse(
                page.getMessages().stream()
                        .sorted(Comparator.comparing(Message::getCreatedAt))
                        .map(MessageMapper::toDto)
                        .toList(),
                page.getNextPageState()
        );
    }

    public MessagePageResponse getConversationsHeaders(String userId, String pageState) {
        MessagePage headers = messageRepository.findConversationsHeaders(userId, pageState, 10);

        return new MessagePageResponse(
                headers.getMessages().stream()
                        .map(MessageMapper::toDto)
                        .toList(),
                headers.getNextPageState()
        );
    }

}

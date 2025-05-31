package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.MessagePageResponse;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.util.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    public Message saveMessage(ChatMessage chatMessage) {
        Message message = new Message();
        message.setRoomId(generateRoomId(chatMessage));
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
        String roomId = generateRoomId(userId1, userId2);
        var page = messageRepository.findByRoomId(roomId, pageState);
        return new MessagePageResponse(
                page.getMessages().stream().map(MessageMapper::toDto).toList(), page.getNextPageState()
        );
    }

    /**
     * @see MessageService#generateRoomId(String, String)
     */
    private String generateRoomId(ChatMessage chatMessage) {
        return generateRoomId(chatMessage.getSender().id(), chatMessage.getRecipient().id());
    }

    /**
     * Generates consistent private message room ID.<br>
     * Sorts the IDs in alphabetic order and combine them with a colon ':'.
     */
    private String generateRoomId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0 ? userId1 + ":" + userId2 : userId2 + ":" + userId1;
    }

}

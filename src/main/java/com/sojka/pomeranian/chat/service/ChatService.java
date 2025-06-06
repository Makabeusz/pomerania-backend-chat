package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.MessagePage;
import com.sojka.pomeranian.chat.dto.MessagePageResponse;
import com.sojka.pomeranian.chat.dto.Pagination;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.repository.ConversationsRepository;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.MessageMapper;
import com.sojka.pomeranian.chat.util.PaginationMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final ConversationsRepository conversationsRepository;

    /**
     * Saves message to AstraDB and sender + recipient conversations to Postgres.
     *
     * @param chatMessage The message got from the user chat
     * @return Saved {@link Message}
     */
    public Message saveMessage(ChatMessage chatMessage) {
        String roomId = CommonUtils.generateRoomId(chatMessage);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Message message = new Message();
        message.setRoomId(roomId);
        message.setCreatedAt(now);
        message.setMessageId(UUID.randomUUID().toString());
        message.setProfileId(chatMessage.getSender().id());
        message.setUsername(chatMessage.getSender().username());
        message.setRecipientProfileId(chatMessage.getRecipient().id());
        message.setRecipientUsername(chatMessage.getRecipient().username());
        message.setContent(chatMessage.getContent());
        message.setMessageType(chatMessage.getType().toString());

        var savedMessage = messageRepository.save(message);

        var senderConversation = new Conversation(new Conversation.Id(chatMessage.getSender().id(), roomId), now);
        var recipientConversation = new Conversation(new Conversation.Id(chatMessage.getRecipient().id(), roomId), now);

        conversationsRepository.saveAll(List.of(senderConversation, recipientConversation));

        return savedMessage;
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

    public MessagePageResponse getConversationsHeaders(String userId, @NonNull String pageState) {
        var pagination = PaginationMapper.toPagination(pageState);
        List<Conversation> conversations = conversationsRepository.findByIdUserId(
                userId, PageRequest.of(pagination.pageNumber(), pagination.pageSize(),
                        Sort.by(Sort.Direction.DESC, "lastMessageAt"))
        );

        var headers = conversations.stream()
                .map(c -> messageRepository.findByRoomId(c.getId().getRoomId(), null, 1))
                .map(MessagePage::getMessages)
                .flatMap(Collection::stream)
                .map(MessageMapper::toDto)
                .toList();

        pageState = conversations.size() == pagination.pageSize()
                ? PaginationMapper.toEncodedString(new Pagination(pagination.pageNumber() + 1, pagination.pageSize()))
                : null;

        return new MessagePageResponse(headers, pageState);
    }

}

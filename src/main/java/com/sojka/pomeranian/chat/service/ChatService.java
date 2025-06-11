package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessagePage;
import com.sojka.pomeranian.chat.dto.MessagePageResponse;
import com.sojka.pomeranian.chat.dto.MessageSaveResult;
import com.sojka.pomeranian.chat.dto.Pagination;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.model.Notification;
import com.sojka.pomeranian.chat.repository.ConversationsRepository;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.repository.NotificationRepository;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import com.sojka.pomeranian.chat.util.mapper.PaginationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.sojka.pomeranian.chat.util.CommonUtils.getCurrentInstant;
import static com.sojka.pomeranian.chat.util.CommonUtils.getRecipientIdFromRoomId;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    // todo: make it a property or always read from frontend
    private static final int CONVERSATIONS_PAGE_SIZE = 10;

    private final MessageRepository messageRepository;
    private final ConversationsRepository conversationsRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Saves message to AstraDB.<br>
     * Also, saves sender and recipient conversations to Postgres to allow to fetch conversations headers.<br>
     * If user is not online then additionally saves the AstraDB notification for the recipient.
     *
     * @param chatMessage The message got from the user chat
     * @return {@link MessageSaveResult} with saved message and notification if recipient is not online
     */
    public MessageSaveResult saveMessage(ChatMessage chatMessage, String roomId, boolean isOnline) {
        Instant now = getCurrentInstant();
        var message = Message.builder()
                .roomId(roomId)
                .createdAt(now)
                .profileId(chatMessage.getSender().id())
                .username(chatMessage.getSender().username())
                .recipientProfileId(chatMessage.getRecipient().id())
                .recipientUsername(chatMessage.getRecipient().username())
                .content(chatMessage.getContent())
                .readAt(isOnline ? now : null)
                .build();

        var savedMessage = messageRepository.save(message);

        var senderConversation = new Conversation(new Conversation.Id(chatMessage.getSender().id(), roomId), now);
        var recipientConversation = new Conversation(new Conversation.Id(chatMessage.getRecipient().id(), roomId), now);

        Notification notification = null;

        log.info("saveMessage: online={}, messageContent={}", isOnline, chatMessage.getContent());
        if (!isOnline) {
            notification = notificationRepository.save(Notification.builder()
                    .profileId(chatMessage.getRecipient().id())
                    .createdAt(now)
                    .senderId(chatMessage.getSender().id())
                    .senderUsername(chatMessage.getSender().username())
                    .content(chatMessage.getContent())
                    .build());
        }

        conversationsRepository.saveAll(List.of(senderConversation, recipientConversation));

        return new MessageSaveResult(savedMessage, notification);
    }

    public Instant markRead(MessageKey keys) {
        var readAt = messageRepository.markRead(keys);

        notificationRepository.deleteAllByPrimaryKeys(
                getRecipientIdFromRoomId(keys.roomId(), keys.profileId()), keys.createdAt(), keys.profileId()
        );

        return readAt;
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
        log.info("Getting conversation headers for user_id={} pageState={}", userId, pageState);
        Pagination pagination;
        if (pageState != null) {
            pagination = PaginationMapper.toPagination(pageState);
        } else {
            pagination = new Pagination(0, CONVERSATIONS_PAGE_SIZE);
        }

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

    public Long countNotifications(String userId) {
        return notificationRepository.countByProfileId(userId).orElseThrow();
    }

}

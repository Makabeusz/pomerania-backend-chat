package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessageNotificationDto;
import com.sojka.pomeranian.chat.dto.MessageSaveResult;
import com.sojka.pomeranian.chat.dto.NotificationHeaderDto;
import com.sojka.pomeranian.chat.dto.Pagination;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.model.MessageNotification;
import com.sojka.pomeranian.chat.repository.ConversationsRepository;
import com.sojka.pomeranian.chat.repository.MessageNotificationRepository;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import com.sojka.pomeranian.chat.util.mapper.NotificationMapper;
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
import static com.sojka.pomeranian.chat.util.mapper.PaginationUtils.createPageState;
import static com.sojka.pomeranian.chat.util.mapper.PaginationUtils.pageStateToPagination;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    // todo: make it a property or always read from frontend
    private static final int CONVERSATIONS_PAGE_SIZE = 10;

    private final MessageRepository messageRepository;
    private final ConversationsRepository conversationsRepository;
    private final MessageNotificationRepository messageNotificationRepository;

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

        MessageNotification notification = null;

        log.info("saveMessage: online={}, messageContent={}", isOnline, chatMessage.getContent());
        if (!isOnline) {
            // Keep maximum 100 chars for message notification content
            String contentSlice = chatMessage.getContent().length() > 96
                    ? chatMessage.getContent().substring(0, 97) + " ..."
                    : chatMessage.getContent();
            notification = messageNotificationRepository.save(
                    new MessageNotification(new MessageNotification.Id(chatMessage.getRecipient().id(), CommonUtils.formatToLocalDateTime(now), chatMessage.getSender().id()),
                            chatMessage.getSender().username(), contentSlice)
            );
        }

        conversationsRepository.saveAll(List.of(senderConversation, recipientConversation));

        return new MessageSaveResult(savedMessage, notification);
    }

    public Instant markRead(MessageKey keys) {
        var readAt = messageRepository.markRead(keys);
        String senderId = getRecipientIdFromRoomId(keys.roomId(), keys.profileId());

        var ids = keys.createdAt().stream()
                .map(createdAt -> new MessageNotification.Id(
                        senderId, CommonUtils.formatToLocalDateTime(createdAt), keys.profileId())
                )
                .toList();

        messageNotificationRepository.deleteAllByIdInBatch(ids);

        return readAt;
    }

    public ResultsPage<ChatMessagePersisted> getConversation(String userId1, String userId2, String pageState) {
        String roomId = CommonUtils.generateRoomId(userId1, userId2);
        var page = messageRepository.findByRoomId(roomId, pageState, 10);
        return new ResultsPage<>(
                page.getResults().stream()
                        .sorted(Comparator.comparing(Message::getCreatedAt))
                        .map(MessageMapper::toDto)
                        .toList(),
                page.getNextPageState()
        );
    }

    public ResultsPage<ChatMessagePersisted> getConversationsHeaders(String userId, String pageState) {
        log.info("Getting conversation headers for user_id={} pageState={}", userId, pageState);
        Pagination pagination = pageStateToPagination(pageState, CONVERSATIONS_PAGE_SIZE);

        List<Conversation> conversations = conversationsRepository.findByIdUserId(
                userId, PageRequest.of(pagination.pageNumber(), pagination.pageSize(),
                        Sort.by(Sort.Direction.DESC, "lastMessageAt"))
        );

        var headers = conversations.stream()
                .map(c -> messageRepository.findByRoomId(c.getId().getRoomId(), null, 1))
                .map(ResultsPage::getResults)
                .flatMap(Collection::stream)
                .map(MessageMapper::toDto)
                .toList();

        pageState = createPageState(conversations.size(), pagination.pageSize(), pagination);

        return new ResultsPage<>(headers, pageState);
    }

    public Long countNotifications(String userId) {
        var count = messageNotificationRepository.countByIdProfileId(userId).orElseThrow();
        log.info("Fetched {} unread message count", count);
        return count;
    }

    public ResultsPage<MessageNotificationDto> getMessageNotifications(String userId, String pageState) {
        Pagination pagination = pageStateToPagination(pageState, 10);

        List<MessageNotification> notifications = messageNotificationRepository.findByIdProfileId(userId, PageRequest.of(
                pagination.pageNumber(), pagination.pageSize(), Sort.by(Sort.Direction.DESC, "id.createdAt")
        ));

        pageState = createPageState(notifications.size(), pagination.pageSize(), pagination);
        var notificationsDto = notifications.stream()
                .map(NotificationMapper::toDto)
                .toList();

        return new ResultsPage<>(notificationsDto, pageState);
    }

    public ResultsPage<NotificationHeaderDto> getMessageNotificationHeaders(String userId, String pageState) {
        Pagination pagination = pageStateToPagination(pageState, 10);

        var headers = messageNotificationRepository.findNotificationsHeaders(userId, PageRequest.of(
                pagination.pageNumber(), pagination.pageSize(), Sort.by(Sort.Direction.DESC, "created_at")
        ));

        pageState = createPageState(headers.size(), pagination.pageSize(), pagination);
        var results = headers.stream()
                .map(NotificationMapper::toDto)
                .toList();

        return new ResultsPage<>(results, pageState);
    }


}

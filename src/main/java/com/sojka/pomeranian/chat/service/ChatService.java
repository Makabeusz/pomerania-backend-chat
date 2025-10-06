package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.ChatResponse;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessageSaveResult;
import com.sojka.pomeranian.chat.dto.MessageType;
import com.sojka.pomeranian.chat.dto.R2BucketDeleteRequest;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.model.MessageNotification;
import com.sojka.pomeranian.chat.repository.ConversationsRepository;
import com.sojka.pomeranian.chat.repository.MessageNotificationRepository;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import com.sojka.pomeranian.chat.util.mapper.NotificationMapper;
import com.sojka.pomeranian.lib.dto.NotificationDto;
import com.sojka.pomeranian.lib.dto.Pagination;
import com.sojka.pomeranian.lib.producerconsumer.ObjectProvider;
import com.sojka.pomeranian.lib.util.JsonUtils;
import com.sojka.pomeranian.pubsub.R2BucketDeletePublisher;
import com.sojka.pomeranian.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.sojka.pomeranian.chat.util.Constants.DM_DESTINATION;
import static com.sojka.pomeranian.lib.util.CommonUtils.generateRoomId;
import static com.sojka.pomeranian.lib.util.CommonUtils.getRecipientIdFromRoomId;
import static com.sojka.pomeranian.lib.util.CommonUtils.noSuchElementException;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.getCurrentInstant;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.getCurrentInstantString;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toLocalDateTime;
import static com.sojka.pomeranian.lib.util.PaginationUtils.createPageState;
import static com.sojka.pomeranian.lib.util.PaginationUtils.pageStateToPagination;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    // todo: make it a property or always read from frontend
    private static final int CONVERSATIONS_PAGE_SIZE = 10;

    @Value("${pomeranian.chat.purge.batch-size}")
    private int purgeBatchSize;

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationsRepository conversationsRepository;
    private final MessageNotificationRepository messageNotificationRepository;
    private final ObjectProvider<Integer, Conversation> unreadMessageSupplier;
    private final R2BucketDeletePublisher deletePublisher;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Saves message to AstraDB.<br>
     * Also, saves sender and recipient conversations to Postgres to allow to fetch conversations headers.<br>
     * If user is not online then additionally saves the AstraDB notification for the recipient.
     *
     * @param chatMessage The message got from the user chat
     * @return {@link MessageSaveResult} with saved message and notification if recipient is not online
     */
    public MessageSaveResult saveMessage(ChatMessage chatMessage, String roomId, boolean isOnline) {
        log.trace("saveMessage input: message={}, roomId={}, isOnline={}", chatMessage, roomId, isOnline);
        var now = getCurrentInstant();
        var recipientId = chatMessage.getRecipient().id();
        var senderId = chatMessage.getSender().id();
        var message = Message.builder()
                .roomId(roomId)
                .createdAt(now)
                .profileId(senderId)
                .username(chatMessage.getSender().username())
                .recipientProfileId(recipientId)
                .resourceId(chatMessage.getResource() != null ? chatMessage.getResource().getId() : null)
                .resourceType(chatMessage.getResource() != null ? chatMessage.getResource().getType() : null)
                .recipientUsername(chatMessage.getRecipient().username())
                .content(chatMessage.getContent())
                .readAt(isOnline ? now : null)
                .build();

        var savedMessage = messageRepository.save(message);

        MessageNotification notification = null;

        log.info("saveMessage: online={}, messageContent={}", isOnline, chatMessage.getContent());
        if (!isOnline) {
            // Keep maximum 100 chars for message notification content
            String contentSlice = chatMessage.getContent().length() > 96
                    ? chatMessage.getContent().substring(0, 97) + " ..."
                    : chatMessage.getContent();
            notification = messageNotificationRepository.save(
                    new MessageNotification(new MessageNotification.Id(recipientId, toLocalDateTime(now), senderId),
                            chatMessage.getSender().username(), contentSlice)
            );
        }

        // TODO: PS.2. It should pull images together with conversation headers or else. It might replace it when users are changing their image192, simply by recipientId column.
        // after adding sender/recipient image192 it will be useless
//        var recipientImage = notificationRepository.findImage192(chatMessage.getRecipient().image192()).orElse(null);
        conversationsRepository.updateLastMessageAt(senderId, recipientId, now);

        return new MessageSaveResult(savedMessage, notification);
    }

    public Instant markRead(MessageKey keys) {
        var readAt = messageRepository.markRead(keys);
        String senderId = getRecipientIdFromRoomId(keys.roomId(), keys.profileId());

        var ids = keys.createdAt().stream()
                .map(createdAt -> new MessageNotification.Id(
                        senderId, toLocalDateTime(createdAt), keys.profileId())
                )
                .toList();

        messageNotificationRepository.deleteAllByIdInBatch(ids);

        return readAt;
    }

    public ResultsPage<ChatMessagePersisted> getConversation(String userId1, String userId2, String pageState) {
        String roomId = generateRoomId(userId1, userId2);
        var page = messageRepository.findByRoomId(roomId, pageState, 10);
        return new ResultsPage<>(
                page.getResults().stream()
                        .sorted(Comparator.comparing(Message::getCreatedAt))
                        .map(MessageMapper::toDto)
                        .toList(),
                page.getNextPageState()
        );
    }

    public ResultsPage<ChatMessagePersisted> getConversationHeaders(String userId, Pagination pagination) {
        log.trace("getConversationsHeaders input: userId={}, pagination={}", userId, pagination);

        List<Conversation> conversations = conversationsRepository.findByIdUserId(
                userId, PageRequest.of(pagination.pageNumber(), pagination.pageSize(),
                        Sort.by(Sort.Direction.DESC, "lastMessageAt"))
        );

        var headers = conversations.stream()
                .map(c -> messageRepository.findByRoomId(CommonUtils.generateRoomId(c), null, 1))
                .map(ResultsPage::getResults)
                .flatMap(Collection::stream)
                .map(MessageMapper::toDto)
                .toList();

        // provide with unread messages count
        List<Pair<Conversation, Integer>> unreadNotificationsCount = unreadMessageSupplier.provide(conversations);
        for (int i = 0; i < unreadNotificationsCount.size(); i++) {
            var pair = unreadNotificationsCount.get(i);
            var metadata = headers.get(i).getMetadata();
            metadata.put("unread", pair.getSecond() + "");
            Optional.ofNullable(pair.getFirst().getStarred())
                    .ifPresent(starred -> metadata.put("starred", starred + ""));
        }

        return new ResultsPage<>(headers, JsonUtils.writeToString(pagination));
    }

    public long getConversationsHeadersCount(String userId) {
        log.trace("getConversationsHeadersCount for userID={}", userId);
        return conversationsRepository.countAllByIdUserId(userId).orElseThrow();
    }

    public boolean updateConversationFlag(String userId, String recipientId, Boolean star) {
        Conversation.Id id = new Conversation.Id(userId, recipientId);
        var conversation = conversationsRepository.findById(id).orElseThrow(noSuchElementException("conversation", id.toString()));
        if (conversation.getStarred() == star) {
            return false;
        }
        conversation.setStarred(star);
        conversationsRepository.save(conversation);
        return true;
    }

    public Long countNotifications(String userId) {
        var count = messageNotificationRepository.countByIdProfileId(userId).orElseThrow();
        log.trace("Fetched {} unread message count", count);
        return count;
    }

    public ResultsPage<NotificationDto> getMessageNotifications(String userId, String pageState) {
        Pagination pagination = pageStateToPagination(pageState, 10);

        List<MessageNotification> notifications = messageNotificationRepository.findByIdProfileId(userId, PageRequest.of(
                pagination.pageNumber(), pagination.pageSize(), Sort.by(Sort.Direction.DESC, "id.createdAt")
        ));

        pageState = createPageState(notifications.size(), pagination);
        var notificationsDto = notifications.stream()
                .map(NotificationMapper::toDto)
                .toList();

        return new ResultsPage<>(notificationsDto, pageState);
    }

    public ResultsPage<NotificationDto> getMessageNotificationHeaders(String userId, String pageState) {
        Pagination pagination = pageStateToPagination(pageState, 10);

        var headers = messageNotificationRepository.findNotificationsHeaders(userId, PageRequest.of(
                pagination.pageNumber(), pagination.pageSize(), Sort.by(Sort.Direction.DESC, "created_at")
        ));

        pageState = createPageState(headers.size(), pagination);
        var results = headers.stream()
                .map(NotificationMapper::toDto)
                .toList();

        return new ResultsPage<>(results, pageState);
    }

    public Set<String> deleteUserInactiveRooms(String userId) {
        Set<String> removedRoomIds = new HashSet<>();
        String pageState = null;
        Pagination pagination;
        List<Conversation> conversations;
        do {
            pagination = pageStateToPagination(pageState, purgeBatchSize);
            conversations = conversationsRepository.findByIdUserId(userId,
                    PageRequest.of(pagination.pageNumber(), pagination.pageSize(),
                            Sort.by(Sort.Direction.DESC, "lastMessageAt"))
            );
            var deadConversations = conversations.stream()
                    .map(Conversation::getId)
                    .map(Conversation.Id::getRecipientId)
                    .filter(recipientId -> !userRepository.existsById(recipientId))
                    .toList();

            removedRoomIds.addAll(deadConversations);
            deadConversations.forEach(messageRepository::deleteRoom);
            pageState = createPageState(conversations.size(), pagination);
        } while (conversations.size() == purgeBatchSize);
        log.info("Removed {} conversation rooms of userID={}", removedRoomIds.size(), userId);
        return removedRoomIds;
    }

    @Transactional
    public long deleteUserConversations(String userId) {
        var deletedUserConversations = conversationsRepository.countAllByIdUserId(userId).orElseThrow();
        conversationsRepository.deleteAllByIdUserId(userId);
        log.info("Removed {} conversations of userID={}", deletedUserConversations, userId);
        return deletedUserConversations;
    }

    @Transactional
    public long deleteUserMessageNotifications(String userId) {
        var deletedUserMessageNotifications = messageNotificationRepository.countByIdProfileId(userId).orElseThrow();
        messageNotificationRepository.deleteAllByIdProfileId(userId);
        log.info("Removed {} message notifications of userID={}", deletedUserMessageNotifications, userId);
        return deletedUserMessageNotifications;
    }

    public int getUnreadMessagesCount(String userId, String roomId) {
        if (roomId == null || roomId.length() != 73) {
            throw new IllegalArgumentException(roomId);
        }
        String senderId = getRecipientIdFromRoomId(roomId, userId);
        return messageNotificationRepository.countByIdProfileIdAndIdSenderId(userId, senderId)
                .map(Long::intValue)
                .orElse(0);
    }

    public boolean deleteMessageResource(String roomId, String createdAt, String profileId, String userId) {
        var message = messageRepository.findById(roomId, createdAt, profileId)
                .orElseThrow(noSuchElementException(
                        "Message", new MessageRepository.IdState(roomId, createdAt, profileId).toString())
                );
        if (message.getResourceId() == null) {
            throw noSuchElementException("ResourceId", new MessageRepository.IdState(roomId, createdAt, profileId).toString()).get();
        }
        deletePublisher.publish(new R2BucketDeleteRequest(message.getResourceId(), userId));

        var now = getCurrentInstantString();
        message.setEditedAt(now);
        message.setResourceId(null);
        message.setResourceType(null);
        message.getMetadata().put("resource-deleted", now);

        var saved = MessageMapper.toDto(messageRepository.update(message));


        // Update both users chat
        messagingTemplate.convertAndSendToUser(
                saved.getRoomId(), DM_DESTINATION, new ChatResponse<>(saved, MessageType.UPDATE)
        );
        return true;
    }

}

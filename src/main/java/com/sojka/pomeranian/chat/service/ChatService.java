package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.ChatResponse;
import com.sojka.pomeranian.chat.dto.ConversationDto;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessageSaveResult;
import com.sojka.pomeranian.chat.dto.MessageType;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.repository.ConversationsRepository;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.repository.projection.ConversationProjection;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import com.sojka.pomeranian.chat.util.mapper.NotificationMapper;
import com.sojka.pomeranian.lib.dto.ChatUser;
import com.sojka.pomeranian.lib.dto.ConversationFlag;
import com.sojka.pomeranian.lib.dto.NotificationDto;
import com.sojka.pomeranian.lib.dto.Pagination;
import com.sojka.pomeranian.lib.dto.R2BucketDeleteRequest;
import com.sojka.pomeranian.notification.util.ConversationMapper;
import com.sojka.pomeranian.pubsub.R2BucketDeletePublisher;
import com.sojka.pomeranian.security.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.sojka.pomeranian.chat.util.Constants.DM_DESTINATION;
import static com.sojka.pomeranian.lib.dto.ConversationFlag.NORMAL;
import static com.sojka.pomeranian.lib.dto.ConversationFlag.STARRED;
import static com.sojka.pomeranian.lib.util.CommonUtils.generateRoomId;
import static com.sojka.pomeranian.lib.util.CommonUtils.getRecipientIdFromRoomId;
import static com.sojka.pomeranian.lib.util.CommonUtils.noSuchElementException;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.getCurrentInstant;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.getCurrentInstantString;
import static com.sojka.pomeranian.lib.util.PaginationUtils.createPageState;
import static com.sojka.pomeranian.lib.util.PaginationUtils.pageStateToPagination;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${pomeranian.chat.purge.batch-size}")
    private int purgeBatchSize;

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationsRepository conversationsRepository;
    private final R2BucketDeletePublisher deletePublisher;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Saves message to AstraDB and publish it back to the websocket.<br>
     * Also, saves sender and recipient conversations to Postgres to allow to fetch conversations headers.<br>
     * The recipient conversation updates its unread count if recipient has no this chat online to serve as a
     * message notification.
     *
     * @param chatMessage The message got from the user chat
     * @return {@link ChatMessagePersisted}
     */
    public MessageSaveResult saveMessage(ChatMessage chatMessage, String roomId, boolean isRecipientOnline) {
        log.trace("saveMessage input: message={}, roomId={}, isOnline={}", chatMessage, roomId, isRecipientOnline);
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
                .readAt(isRecipientOnline ? now : null)
                .build();

        var savedMessage = MessageMapper.toDto(messageRepository.save(message));
        savedMessage.addMetadata("senderImage192", chatMessage.getSender().image192() + "");

        // Update both users chat
        messagingTemplate.convertAndSendToUser(roomId, DM_DESTINATION, new ChatResponse<>(savedMessage));

        log.trace("savedMessage: isRecipientOnline={}, messageContent={}", isRecipientOnline, chatMessage.getContent());

        var contentType = Conversation.ContentType.getTypeByMessageData(message);
        String contentSlice = chatMessage.getContent().length() > 96
                ? chatMessage.getContent().substring(0, 97) + "..."
                : chatMessage.getContent();

        var senderConversation = getExistingOrNewConversation(senderId, recipientId, now, contentSlice, contentType, true);
        senderConversation.setUnreadCount(0); // for extra safety, no unread if user is actively writing in the chat
        conversationsRepository.save(senderConversation);
        log.trace("Updated sender conversation: {}", senderConversation);

        var recipientConversation = getExistingOrNewConversation(recipientId, senderId, now, contentSlice, contentType, false);
        recipientConversation.setUnreadCount(isRecipientOnline ? 0 : recipientConversation.getUnreadCount() + 1);
        conversationsRepository.save(recipientConversation);
        log.trace("Updated recipient conversation: {}", recipientConversation);

        return new MessageSaveResult(savedMessage, ConversationMapper.toDto(recipientConversation, new ChatUser(
                chatMessage.getRecipient().id(), chatMessage.getRecipient().username(), chatMessage.getRecipient().image192()
        )));
    }

    private Conversation getExistingOrNewConversation(
            UUID userId,
            UUID recipientId,
            Instant lastMessageAt,
            String content,
            Conversation.ContentType contentType,
            boolean isUserConversation
    ) {
        Conversation.Id conversationId = new Conversation.Id(userId, recipientId);
        var conversation = conversationsRepository.findById(conversationId)
                .orElse(Conversation.builder().id(conversationId)
                        .unreadCount(0)
                        .flag(NORMAL)
                        .build());
        conversation.setLastMessageAt(lastMessageAt);
        conversation.setContent(content);
        conversation.setContentType(contentType);
        conversation.setIsLastMessageFromUser(isUserConversation);
        return conversation;
    }

    public Instant markRead(MessageKey keys) {
        var readAt = messageRepository.markRead(keys);
        UUID senderId = getRecipientIdFromRoomId(keys.roomId(), keys.profileId());
        try {
            conversationsRepository.updateUnreadCount(senderId, keys.profileId(), 0);
        } catch (Exception e) {
            log.warn("Mark read conversation update failed: {}", e.getMessage());
        }

        return readAt;
    }

    public ResultsPage<ChatMessagePersisted> getConversationMessages(UUID userId, UUID otherProfileId, String pageState) {
        String roomId = generateRoomId(userId, otherProfileId);
        var page = messageRepository.findByRoomId(roomId, pageState, 20);
        return new ResultsPage<>(
                page.getResults().stream()
                        .sorted(Comparator.comparing(Message::getCreatedAt))
                        .map(MessageMapper::toDto)
                        .toList(),
                page.getNextPageState()
        );
    }

    public ResultsPage<ConversationDto> getConversations(
            UUID userId, @NonNull ConversationFlag flag, Pagination pagination
    ) {
        log.trace("getConversations input: userId={}, flag={}, pagination={}", userId, flag, pagination);
        Sort sortBy = Sort.by("last_message_at").descending();
        PageRequest pageRequest = pagination == null
                ? PageRequest.of(0, 10, sortBy)
                : PageRequest.of(pagination.pageNumber(), pagination.pageSize(), sortBy);
        List<ConversationProjection> conversations;
        if (flag == NORMAL) {
            conversations = conversationsRepository.findByUserIdAndFlags(userId, NORMAL, STARRED, pageRequest);
        } else {
            conversations = conversationsRepository.findByUserIdAndFlag(userId, flag, pageRequest);
        }

        return new ResultsPage<>(
                conversations.stream().map(ConversationMapper::toDto).toList(),
                createPageState(conversations.size(), pagination)
        );
    }

    public long getConversationsCount(UUID userId, ConversationFlag flag) {
        if (flag == NORMAL) {
            return conversationsRepository.countAllByIdUserIdAndFlagOrFlag(userId, NORMAL, STARRED);
        } else {
            return conversationsRepository.countAllByIdUserIdAndFlag(userId, flag);
        }
    }

    public boolean updateConversationFlag(UUID userId, UUID recipientId, ConversationFlag flag) {
        Conversation.Id id = new Conversation.Id(userId, recipientId);
        var conversation = conversationsRepository.findById(id).orElseThrow(noSuchElementException("conversation", id.toString()));
        if (conversation.getFlag() == flag) {
            return false;
        }
        conversation.setFlag(flag);
        conversationsRepository.save(conversation);
        return true;
    }

    public Long countNotifications(UUID userId) {
        var count = conversationsRepository.sumUnreadCountByUserId(userId);
        log.trace("Fetched {} unread message count", count);
        return count;
    }

    public ResultsPage<NotificationDto> getMessageNotifications(UUID userId, String pageState) {
        Pagination pagination = pageStateToPagination(pageState, 10);

        var headers = conversationsRepository.findNotifications(userId, PageRequest.of(
                pagination.pageNumber(), pagination.pageSize(), Sort.by(Sort.Direction.DESC, "last_message_at")
        ));

        pageState = createPageState(headers.size(), pagination);
        var results = headers.stream()
                .map(NotificationMapper::toDto)
                .toList();

        return new ResultsPage<>(results, pageState);
    }

    public Set<String> deleteUserInactiveRooms(UUID userId) {
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
                    .map(id -> generateRoomId(userId, id))
                    .toList();

            removedRoomIds.addAll(deadConversations);
            deadConversations.forEach(messageRepository::deleteRoom);
            pageState = createPageState(conversations.size(), pagination);
        } while (conversations.size() == purgeBatchSize);
        log.info("Removed {} conversation rooms of userID={}", removedRoomIds.size(), userId);
        return removedRoomIds;
    }

    @Transactional
    public void deleteUserConversations(UUID userId) {
        conversationsRepository.deleteAllByIdUserId(userId);
        log.info("Removed all conversations of userID={}", userId);
    }

    public long getRoomUnreadMessagesCount(UUID userId, String roomId) {
        if (roomId == null || roomId.length() != 73) {
            throw new IllegalArgumentException(roomId);
        }
        UUID recipientId = getRecipientIdFromRoomId(roomId, userId);
        return conversationsRepository.findUnreadCountByIdUserIdAndIdRecipientId(userId, recipientId);
    }

    public boolean deleteMessageResource(String roomId, String createdAt, UUID profileId, UUID userId) {
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

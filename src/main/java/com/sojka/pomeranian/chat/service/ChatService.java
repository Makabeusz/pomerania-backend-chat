package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.ChatResponse;
import com.sojka.pomeranian.chat.dto.ConversationDto;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessageType;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.repository.ConversationsRepository;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.repository.projection.ConversationProjection;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import com.sojka.pomeranian.chat.util.mapper.NotificationMapper;
import com.sojka.pomeranian.lib.dto.ConversationFlag;
import com.sojka.pomeranian.lib.dto.Notification;
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

import static com.sojka.pomeranian.chat.dto.MessageType.REVALIDATE;
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
     * @return createdAt in string
     */
    public String processMessage(ChatMessage chatMessage, String roomId, boolean isRecipientOnline) {
        log.trace("saveMessage input: message={}, roomId={}, isOnline={}", chatMessage, roomId, isRecipientOnline);
        var now = getCurrentInstant();
        var recipientId = chatMessage.getRecipient().getId();
        var senderId = chatMessage.getSender().getId();
        var message = Message.builder()
                .roomId(roomId)
                .createdAt(now)
                .profileId(senderId)
                .resourceId(chatMessage.getResource() != null ? chatMessage.getResource().getId() : null)
                .resourceType(chatMessage.getResource() != null ? chatMessage.getResource().getType() : null)
                .resourceHeight(chatMessage.getResource() != null ? chatMessage.getResource().getHeight() : null)
                .resourceWidth(chatMessage.getResource() != null ? chatMessage.getResource().getWidth() : null)
                .thumbnailId(chatMessage.getResource() != null ? chatMessage.getResource().getThumbnailId() : null)
                .content(chatMessage.getContent())
                .readAt(isRecipientOnline ? now : null)
                .build();

        var savedMessage = MessageMapper.toDto(messageRepository.save(message));

        // Update both users chat - intentionally sent immediately for lowest latency (see review).
        // Conversation header updates below are best-effort only.
        messagingTemplate.convertAndSendToUser(roomId, DM_DESTINATION, new ChatResponse<>(savedMessage));

        log.trace("savedMessage: isRecipientOnline={}, messageContent={}", isRecipientOnline, chatMessage.getContent());

        // Best-effort Postgres conversation header updates. We deliberately do NOT use @Transactional here
        // to preserve low message delivery latency. Failures are logged at ERROR so they are observable,
        // but the message has already been delivered to participants.
        try {
            var contentType = Conversation.ContentType.getTypeByMessageData(message);
            String content = chatMessage.getContent();
            String contentSlice = (content != null && content.length() > 96)
                    ? content.substring(0, 97) + "..."
                    : content;

            var senderConversation = getExistingOrNewConversation(senderId, recipientId, now, contentSlice, contentType, true);
            senderConversation.setUnreadCount(0); // for extra safety, no unread if user is actively writing in the chat
            conversationsRepository.save(senderConversation);
            log.trace("Updated sender conversation: {}", senderConversation);

            var recipientConversation = getExistingOrNewConversation(recipientId, senderId, now, contentSlice, contentType, false);
            recipientConversation.setUnreadCount(isRecipientOnline ? 0 : recipientConversation.getUnreadCount() + 1);
            conversationsRepository.save(recipientConversation);
            log.trace("Updated recipient conversation: {}", recipientConversation);
        } catch (Exception e) {
            log.error("Failed to update Postgres conversation headers after successful WS delivery. " +
                            "Message is visible to users but may be temporarily missing from /headers lists until next activity. " +
                            "roomId={}, senderId={}, recipientId={}",
                    roomId, senderId, recipientId, e);
        }

        return savedMessage.getCreatedAt();
    }

    public void processRefreshRequest(String roomId) {
        messagingTemplate.convertAndSendToUser(roomId, DM_DESTINATION, new ChatResponse<>(REVALIDATE));
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
        return messageRepository.markRead(keys);
    }

    public Long resetConversationUnreadCount(UUID userId, UUID recipientId) {
        Long previousCount = conversationsRepository.findUnreadCountByIdUserIdAndIdRecipientId(userId, recipientId);
        if (previousCount != null && previousCount > 0) {
            conversationsRepository.updateUnreadCount(userId, recipientId, 0);
        }
        return previousCount;
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

    public List<ConversationDto> getConversations(
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

        return conversations.stream().map(ConversationMapper::toDto).toList();
    }

    public Long getConversationsCount(UUID userId, ConversationFlag flag) {
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
        log.debug("User updated conversation flag: userId={}, recipientId={}, flag={}", userId, recipientId, flag);
        return true;
    }

    public Long countNotifications(UUID userId) {
        var count = conversationsRepository.sumUnreadCountByUserId(userId);
        log.trace("Fetched {} unread message count", count);
        return count;
    }

    public ResultsPage<Notification<Object>> getMessageNotifications(UUID userId, String pageState, Integer pageSize) {
        Pagination pagination = pageStateToPagination(pageState, pageSize);

        var headers = conversationsRepository.findNotifications(userId, PageRequest.of(
                pagination.pageNumber(), pagination.pageSize(), Sort.by(Sort.Direction.DESC, "last_message_at")
        ));

        pageState = createPageState(headers.size(), pagination);
        var results = headers.stream()
                .map(NotificationMapper::toNotification)
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

            for (String deadRoomId : deadConversations) {
                try {
                    removedRoomIds.add(deadRoomId);
                    messageRepository.deleteRoom(deadRoomId);
                } catch (Exception e) {
                    // Do not let one bad room abort the entire purge for this user.
                    log.error("Failed to delete inactive room during user purge. " +
                                    "The room may still contain messages in Astra. roomId={}, userId={}",
                            deadRoomId, userId, e);
                    // Remove from success set if we added it optimistically
                    removedRoomIds.remove(deadRoomId);
                }
            }
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

    public Long getRoomUnreadMessagesCount(UUID userId, String roomId) {
        if (roomId == null || roomId.length() != 73) {
            throw new IllegalArgumentException(roomId);
        }
        UUID recipientId = getRecipientIdFromRoomId(roomId, userId);
        return conversationsRepository.findUnreadCountByIdUserIdAndIdRecipientId(userId, recipientId);
    }

    public boolean deleteMessageResource(String roomId, String createdAt, UUID userId) {
        var message = messageRepository.findById(roomId, createdAt, userId)
                .orElseThrow(noSuchElementException(
                        "Message", new MessageRepository.IdState(roomId, createdAt, userId).toString())
                );
        if (message.getResourceId() == null) {
            throw noSuchElementException("ResourceId", new MessageRepository.IdState(roomId, createdAt, userId).toString()).get();
        }
        UUID deletedResourceId = message.getResourceId();
        deletePublisher.publish(new R2BucketDeleteRequest(deletedResourceId, userId));

        // Best-effort update of the message record + WS notification after publishing the R2 delete request.
        // We log at ERROR on failure so that orphaned resources in the bucket can be identified and cleaned manually.
        try {
            var now = getCurrentInstantString();
            message.setEditedAt(now);
            message.setResourceId(null);
            message.setResourceType(null);
            if (message.getMetadata() == null) {
                message.setMetadata(new java.util.HashMap<>());
            }
            message.getMetadata().put("resource-deleted", now);

            var saved = MessageMapper.toDto(messageRepository.update(message));

            // Update both users chat
            messagingTemplate.convertAndSendToUser(
                    saved.getRoomId(), DM_DESTINATION, new ChatResponse<>(saved, MessageType.UPDATE)
            );
            log.info("User deleted chat-resource: userId={}, roomId={}, deletedResourceId={}", userId, roomId, deletedResourceId);
        } catch (Exception e) {
            log.error("Failed to update message / notify clients after publishing R2 delete request. " +
                            "The resource blob may be orphaned in storage and require manual cleanup. " +
                            "roomId={}, createdAt={}, userId={}, resourceId={}",
                    roomId, createdAt, userId, deletedResourceId, e);
        }
        return true;
    }

}

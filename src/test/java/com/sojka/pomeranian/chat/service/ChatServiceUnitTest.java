package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.ChatResponse;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessageType;
import com.sojka.pomeranian.chat.dto.UserId;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.repository.ConversationsRepository;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.lib.dto.R2BucketDeleteRequest;
import com.sojka.pomeranian.pubsub.R2BucketDeletePublisher;
import com.sojka.pomeranian.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.sojka.pomeranian.chat.util.Constants.DM_DESTINATION;
import static com.sojka.pomeranian.lib.dto.ConversationFlag.NORMAL;
import static com.sojka.pomeranian.lib.dto.ConversationFlag.STARRED;
import static com.sojka.pomeranian.lib.util.CommonUtils.generateRoomId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationsRepository conversationsRepository;
    @Mock
    private R2BucketDeletePublisher deletePublisher;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatService chatService;

    private static final int DEFAULT_BATCH = 50;

    UUID userId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    String roomId = generateRoomId(userId, otherId);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatService, "purgeBatchSize", DEFAULT_BATCH);
    }

    // --- processMessage ---

    @Test
    void processMessage_basic_offlineRecipient_savesMessageAndBothConvosIncrementsUnread() {
        ChatMessage msg = ChatMessage.basicBuilder()
                .content("Hello there")
                .sender(new UserId(userId))
                .recipient(new UserId(otherId))
                .build();

        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        String created = chatService.processMessage(msg, roomId, false);

        assertNotNull(created);

        ArgumentCaptor<Message> msgCap = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(msgCap.capture());
        Message savedMsg = msgCap.getValue();
        assertEquals(roomId, savedMsg.getRoomId());
        assertEquals(userId, savedMsg.getProfileId());
        assertEquals("Hello there", savedMsg.getContent());
        assertNull(savedMsg.getReadAt()); // offline

        // WS broadcast
        ArgumentCaptor<ChatResponse<?>> wsCap = ArgumentCaptor.forClass(ChatResponse.class);
        verify(messagingTemplate).convertAndSendToUser(eq(roomId), eq(DM_DESTINATION), wsCap.capture());
        assertThat(wsCap.getValue().getType()).isEqualTo(MessageType.CHAT);

        // sender convo saved with 0 unread, isLastFromUser=true
        ArgumentCaptor<Conversation> senderCap = ArgumentCaptor.forClass(Conversation.class);
        ArgumentCaptor<Conversation> recipCap = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationsRepository, times(2)).save(any(Conversation.class));
        ArgumentCaptor<Conversation> convoCap = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationsRepository, times(2)).save(convoCap.capture());
        var convos = convoCap.getAllValues();
        assertThat(convos).hasSize(2);
        assertThat(convos.get(0).getUnreadCount() + convos.get(1).getUnreadCount()).isEqualTo(1); // one has +1
    }

    @Test
    void processMessage_onlineRecipient_setsReadAtAndZeroUnreadForBoth() {
        ChatMessage msg = ChatMessage.basicBuilder()
                .content("Hi")
                .sender(new UserId(userId))
                .recipient(new UserId(otherId))
                .build();

        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        chatService.processMessage(msg, roomId, true);

        ArgumentCaptor<Message> msgCap = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(msgCap.capture());
        assertNotNull(msgCap.getValue().getReadAt());

        // Both convos should have unread=0
        ArgumentCaptor<Conversation> cap = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationsRepository, times(2)).save(cap.capture());
        assertThat(cap.getAllValues())
                .extracting(Conversation::getUnreadCount)
                .containsExactly(0, 0);
    }

    @Test
    void processMessage_longContent_truncatesPreviewInConversations() {
        String longContent = "a".repeat(120);
        ChatMessage msg = ChatMessage.basicBuilder()
                .content(longContent)
                .sender(new UserId(userId))
                .recipient(new UserId(otherId))
                .build();

        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        chatService.processMessage(msg, roomId, false);

        ArgumentCaptor<Conversation> cap = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationsRepository, times(2)).save(cap.capture());
        String expectedSlice = longContent.substring(0, 97) + "...";
        assertThat(cap.getAllValues())
                .extracting(Conversation::getContent)
                .containsExactly(expectedSlice, expectedSlice);
    }

    @Test
    void processMessage_withResource_setsCorrectContentType() {
        ChatMessage msg = ChatMessage.basicBuilder()
                .content("Look at this")
                .resource(ChatMessage.Resource.builder().id(UUID.randomUUID()).type("PHOTO").build())
                .sender(new UserId(userId))
                .recipient(new UserId(otherId))
                .build();

        when(messageRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        chatService.processMessage(msg, roomId, false);

        ArgumentCaptor<Conversation> cap = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationsRepository, times(2)).save(cap.capture());
        assertThat(cap.getAllValues())
                .extracting(Conversation::getContentType)
                .containsExactly(Conversation.ContentType.MESSAGE_PHOTO, Conversation.ContentType.MESSAGE_PHOTO);
    }

    // --- processRefreshRequest ---

    @Test
    void processRefreshRequest_sendsRevalidateToRoom() {
        chatService.processRefreshRequest(roomId);

        ArgumentCaptor<ChatResponse<?>> cap = ArgumentCaptor.forClass(ChatResponse.class);
        verify(messagingTemplate).convertAndSendToUser(eq(roomId), eq(DM_DESTINATION), cap.capture());
        assertThat(cap.getValue().getType()).isEqualTo(MessageType.REVALIDATE);
    }

    // --- markRead ---

    @Test
    void markRead_delegatesToRepository() {
        Instant expected = Instant.now();
        MessageKey key = new MessageKey(roomId, List.of(Instant.now()), otherId);
        when(messageRepository.markRead(key)).thenReturn(expected);

        Instant result = chatService.markRead(key);

        assertEquals(expected, result);
        verify(messageRepository).markRead(key);
    }

    // --- resetConversationUnreadCount ---

    @Test
    void resetConversationUnreadCount_nullOrZero_noUpdate_returnsPrevious() {
        when(conversationsRepository.findUnreadCountByIdUserIdAndIdRecipientId(userId, otherId))
                .thenReturn(null);

        Long prev = chatService.resetConversationUnreadCount(userId, otherId);

        assertNull(prev);
        verify(conversationsRepository, never()).updateUnreadCount(any(), any(), any(int.class));

        when(conversationsRepository.findUnreadCountByIdUserIdAndIdRecipientId(userId, otherId))
                .thenReturn(0L);
        prev = chatService.resetConversationUnreadCount(userId, otherId);
        assertEquals(0L, prev);
        verify(conversationsRepository, never()).updateUnreadCount(any(), any(), any(int.class));
    }

    @Test
    void resetConversationUnreadCount_positive_updatesToZero_returnsOld() {
        when(conversationsRepository.findUnreadCountByIdUserIdAndIdRecipientId(userId, otherId))
                .thenReturn(5L);

        Long prev = chatService.resetConversationUnreadCount(userId, otherId);

        assertEquals(5L, prev);
        verify(conversationsRepository).updateUnreadCount(userId, otherId, 0);
    }

    // --- getConversationMessages ---

    @Test
    void getConversationMessages_returnsSortedAscResultsAndPageState() {
        Instant t1 = Instant.now().minusSeconds(10);
        Instant t2 = Instant.now().minusSeconds(5);
        Message m1 = Message.builder().roomId(roomId).createdAt(t1).profileId(userId).content("old").build();
        Message m2 = Message.builder().roomId(roomId).createdAt(t2).profileId(otherId).content("new").build();

        ResultsPage<Message> page = new ResultsPage<>(List.of(m2, m1), "nextState");
        when(messageRepository.findByRoomId(roomId, "ps", 20)).thenReturn(page);

        ResultsPage<ChatMessagePersisted> result = chatService.getConversationMessages(userId, otherId, "ps");

        assertEquals(2, result.getResults().size());
        assertEquals("old", result.getResults().get(0).getContent()); // sorted asc
        assertEquals("new", result.getResults().get(1).getContent());
        assertEquals("nextState", result.getNextPageState());
    }

    // --- getConversations / count ---

    @Test
    void getConversationsCount_normal_usesOrQuery() {
        when(conversationsRepository.countAllByIdUserIdAndFlagOrFlag(userId, NORMAL, STARRED)).thenReturn(7L);

        Long c = chatService.getConversationsCount(userId, NORMAL);

        assertEquals(7L, c);
    }

    @Test
    void getConversationsCount_specificFlag_usesSingleQuery() {
        when(conversationsRepository.countAllByIdUserIdAndFlag(userId, STARRED)).thenReturn(2L);

        assertEquals(2L, chatService.getConversationsCount(userId, STARRED));
    }

    // --- updateConversationFlag ---

    @Test
    void updateConversationFlag_changesFlag_savesAndReturnsTrue() {
        Conversation.Id id = new Conversation.Id(userId, otherId);
        Conversation convo = Conversation.builder().id(id).flag(NORMAL).build();
        when(conversationsRepository.findById(id)).thenReturn(Optional.of(convo));

        boolean changed = chatService.updateConversationFlag(userId, otherId, STARRED);

        assertTrue(changed);
        assertEquals(STARRED, convo.getFlag());
        verify(conversationsRepository).save(convo);
    }

    @Test
    void updateConversationFlag_sameFlag_returnsFalse_noSave() {
        Conversation.Id id = new Conversation.Id(userId, otherId);
        Conversation convo = Conversation.builder().id(id).flag(STARRED).build();
        when(conversationsRepository.findById(id)).thenReturn(Optional.of(convo));

        boolean changed = chatService.updateConversationFlag(userId, otherId, STARRED);

        assertFalse(changed);
        verify(conversationsRepository, never()).save(any());
    }

    @Test
    void updateConversationFlag_notFound_throws() {
        Conversation.Id id = new Conversation.Id(userId, otherId);
        when(conversationsRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.updateConversationFlag(userId, otherId, NORMAL))
                .isInstanceOf(NoSuchElementException.class);
    }

    // --- countNotifications / getMessageNotifications ---

    @Test
    void countNotifications_delegates() {
        when(conversationsRepository.sumUnreadCountByUserId(userId)).thenReturn(42L);
        assertEquals(42L, chatService.countNotifications(userId));
    }

    // --- deleteUserInactiveRooms ---

    @Test
    void deleteUserInactiveRooms_noConversations_returnsEmpty_andNoDeletes() {
        when(conversationsRepository.findByIdUserId(eq(userId), any()))
                .thenReturn(List.of());

        Set<String> removed = chatService.deleteUserInactiveRooms(userId);

        assertThat(removed).isEmpty();
        verify(messageRepository, never()).deleteRoom(any());
    }

    @Test
    void deleteUserInactiveRooms_mixedActiveInactive_deletesOnlyDeadOnes_stopsOnPartialPage() {
        ReflectionTestUtils.setField(chatService, "purgeBatchSize", 2);

        Conversation c1 = convo(userId, UUID.randomUUID()); // active
        Conversation c2 = convo(userId, UUID.randomUUID()); // inactive
        Conversation c3 = convo(userId, UUID.randomUUID()); // inactive but second page

        when(conversationsRepository.findByIdUserId(eq(userId), any()))
                .thenReturn(List.of(c1, c2))   // page 1 full -> continue
                .thenReturn(List.of(c3));      // page 2 partial -> stop

        when(userRepository.existsById(any(UUID.class))).thenReturn(true, false, false); // c1 active, c2/c3 dead

        Set<String> removed = chatService.deleteUserInactiveRooms(userId);

        assertThat(removed).hasSize(2);
        verify(messageRepository, times(2)).deleteRoom(any());
    }

    @Test
    void deleteUserInactiveRooms_activeUsers_noDeletion() {
        Conversation c = convo(userId, otherId);
        when(conversationsRepository.findByIdUserId(eq(userId), any())).thenReturn(List.of(c));
        when(userRepository.existsById(otherId)).thenReturn(true);

        Set<String> removed = chatService.deleteUserInactiveRooms(userId);

        assertThat(removed).isEmpty();
        verify(messageRepository, never()).deleteRoom(any());
    }

    // --- deleteUserConversations ---

    @Test
    void deleteUserConversations_delegates() {
        chatService.deleteUserConversations(userId);
        verify(conversationsRepository).deleteAllByIdUserId(userId);
    }

    // --- getRoomUnreadMessagesCount ---

    @Test
    void getRoomUnreadMessagesCount_validRoomId_returnsCount() {
        String validRoom = generateRoomId(userId, otherId); // 73 chars
        when(conversationsRepository.findUnreadCountByIdUserIdAndIdRecipientId(userId, otherId))
                .thenReturn(3L);

        assertEquals(3L, chatService.getRoomUnreadMessagesCount(userId, validRoom));
    }

    @Test
    void getRoomUnreadMessagesCount_invalidRoomId_throwsIllegalArgument() {
        assertThatThrownBy(() -> chatService.getRoomUnreadMessagesCount(userId, "short"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chatService.getRoomUnreadMessagesCount(userId, "x".repeat(80)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- deleteMessageResource ---

    @Test
    void deleteMessageResource_happyPath_publishesDelete_updatesMessage_sendsWsUpdate_returnsTrue() {
        UUID resId = UUID.randomUUID();
        Message msg = Message.builder()
                .roomId(roomId)
                .createdAt(Instant.now())
                .profileId(userId)
                .resourceId(resId)
                .resourceType("PHOTO")
                .metadata(new java.util.HashMap<>())
                .build();
        Message updated = Message.builder().roomId(roomId).createdAt(msg.getCreatedAt()).profileId(userId).build();

        when(messageRepository.findById(roomId, "2025-01-01T00:00:00Z", userId))
                .thenReturn(Optional.of(msg));
        when(messageRepository.update(any(Message.class))).thenReturn(updated);

        boolean result = chatService.deleteMessageResource(roomId, "2025-01-01T00:00:00Z", userId);

        assertTrue(result);

        ArgumentCaptor<R2BucketDeleteRequest> delCap = ArgumentCaptor.forClass(R2BucketDeleteRequest.class);
        verify(deletePublisher).publish(delCap.capture());
        assertEquals(resId, delCap.getValue().id());
        assertEquals(userId, delCap.getValue().userId());

        ArgumentCaptor<Message> upCap = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).update(upCap.capture());
        Message toUpdate = upCap.getValue();
        assertNull(toUpdate.getResourceId());
        assertNotNull(toUpdate.getEditedAt());
        assertTrue(toUpdate.getMetadata().containsKey("resource-deleted"));

        ArgumentCaptor<ChatResponse<?>> wsCap = ArgumentCaptor.forClass(ChatResponse.class);
        verify(messagingTemplate).convertAndSendToUser(eq(roomId), eq(DM_DESTINATION), wsCap.capture());
        assertEquals(MessageType.UPDATE, wsCap.getValue().getType());
    }

    @Test
    void deleteMessageResource_messageNotFound_throws() {
        when(messageRepository.findById(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                chatService.deleteMessageResource(roomId, "2025-01-01", userId)
        ).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteMessageResource_noResource_throws() {
        Message msg = Message.builder().roomId(roomId).createdAt(Instant.now()).profileId(userId).build();
        when(messageRepository.findById(any(), any(), any())).thenReturn(Optional.of(msg));

        assertThatThrownBy(() ->
                chatService.deleteMessageResource(roomId, "2025-01-01", userId)
        ).isInstanceOf(NoSuchElementException.class);
    }

    // helpers

    private Conversation convo(UUID uid, UUID rid) {
        return Conversation.builder()
                .id(new Conversation.Id(uid, rid))
                .lastMessageAt(Instant.now())
                .content("hi")
                .unreadCount(1)
                .flag(NORMAL)
                .build();
    }

}

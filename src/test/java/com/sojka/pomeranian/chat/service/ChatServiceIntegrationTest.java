package com.sojka.pomeranian.chat.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.sojka.pomeranian.TestcontainersConfiguration;
import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.db.AstraTestcontainersConnector;
import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.ChatUser;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessageNotificationDto;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.model.MessageNotification;
import com.sojka.pomeranian.chat.repository.ConversationsRepository;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.repository.MessageNotificationRepository;
import com.sojka.pomeranian.chat.util.CommonUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import static com.sojka.pomeranian.chat.util.TestUtils.createChatMessage;
import static com.sojka.pomeranian.chat.util.TestUtils.paginationString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import({TestcontainersConfiguration.class})
@SpringBootTest
class ChatServiceIntegrationTest {

    @Autowired
    ChatService chatService;
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    AstraTestcontainersConnector connector;
    @Autowired
    ConversationsRepository conversationsRepository;
    @Autowired
    MessageNotificationRepository messageNotificationRepository;

    CqlSession session;

    @BeforeEach
    void setUp() {
        session = connector.connect();
        session.execute("TRUNCATE messages.messages");
        conversationsRepository.deleteAll();
        messageNotificationRepository.deleteAll();
    }

    @Test
    void saveMessage_message_savedWithBothConversations() {
        ChatMessage chatMessage = ChatMessage.basicBuilder()
                .content("Hello, World!")
                .sender(new ChatUser("user1", "User1"))
                .recipient(new ChatUser("user2", "User2"))
                .build();
        String roomId = CommonUtils.generateRoomId(chatMessage);

        var saved = chatService.saveMessage(chatMessage, roomId, false).message();

        // Verify message in messages table
        SimpleStatement selectMessage = SimpleStatement.newInstance(
                "SELECT * FROM messages.messages WHERE room_id = ? AND created_at = ? AND profile_id = ?",
                saved.getRoomId(), saved.getCreatedAt(), saved.getProfileId()
        );
        var row = connector.getSession()
                .execute(selectMessage)
                .one();
        Message saveResult = Message.builder()
                .roomId(row.getString("room_id"))
                .createdAt(row.getInstant("created_at"))
                .profileId(row.getString("profile_id"))
                .username(row.getString("username"))
                .recipientProfileId(row.getString("recipient_profile_id"))
                .recipientUsername(row.getString("recipient_username"))
                .content(row.getString("content"))
                .resourceId(row.getString("resource_id"))
                .threadId(row.getString("thread_id"))
                .editedAt(row.getString("edited_at"))
                .deletedAt(row.getString("deleted_at"))
                .pinned(row.getBoolean("pinned"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .build();
        assertThat(saveResult).usingRecursiveComparison(new RecursiveComparisonConfiguration())
                .ignoringFields("createdAt", "messageId")
                .isEqualTo(Message.builder()
                        .roomId(roomId)
                        .profileId("user1")
                        .username("User1")
                        .recipientProfileId("user2")
                        .recipientUsername("User2")
                        .content("Hello, World!")
                        .metadata(Collections.emptyMap())
                        .pinned(false)
                        .build());

        assertThat(conversationsRepository.findAll()).containsExactly(
                new Conversation(new Conversation.Id("user1", "user1:user2"), saveResult.getCreatedAt()),
                new Conversation(new Conversation.Id("user2", "user1:user2"), saveResult.getCreatedAt())
        );
    }

    @Test
    void getConversation_fewMessages_sameMessagesInDescOrder() {
        String userId1 = "user1";
        String userId2 = "user2";
        String roomId = userId1 + ":" + userId2;
        Message message1 = createChatMessage(roomId, "Message 1", userId1, userId2, Instant.now().minusSeconds(10));
        Message message2 = createChatMessage(roomId, "Message 2", userId1, userId2, Instant.now().minusSeconds(5));
        Message message3 = createChatMessage(roomId, "Message 3", userId2, userId1, Instant.now());
        messageRepository.save(message1);
        messageRepository.save(message2);
        messageRepository.save(message3);

        var response = chatService.getConversation(userId1, userId2, null);

        assertEquals(3, response.getResults().size());
        assertEquals("Message 1", response.getResults().get(0).getContent()); // Sorted by created_at DESC
        assertEquals("Message 2", response.getResults().get(1).getContent());
        assertEquals("Message 3", response.getResults().get(2).getContent());

        assertNull(response.getNextPageState()); // No more pages
    }

    @ParameterizedTest
    @MethodSource("conversationHeadersSource")
    void getConversationsHeaders_twoConversationsWithFewMessagesEach_twoConversationHeaders(String pagination) {
        String userId = "userX";
        String roomIdXY = "userX:userY";
        String roomIdXZ = "userX:userZ";
        Message message3 = createChatMessage(roomIdXY, "Message 3", "userY", "userX", Instant.now());
        Message message2 = createChatMessage(roomIdXY, "Message 2", "userX", "userY", Instant.now().minusSeconds(5));
        Message message1 = createChatMessage(roomIdXY, "Message 1", "userX", "userY", Instant.now().minusSeconds(10));
        Message message0 = createChatMessage(roomIdXZ, "Message 0", "userZ", "userX", Instant.now().minusSeconds(15));
        Message messageNotInTheScope = createChatMessage("userY:userZ", "Other users message", "userZ", "userY", Instant.now().minusSeconds(15));
        messageRepository.save(messageNotInTheScope);
        messageRepository.save(message0);
        messageRepository.save(message1);
        messageRepository.save(message2);
        messageRepository.save(message3);
        // user conversations
        conversationsRepository.save(new Conversation(new Conversation.Id(userId, roomIdXY), message3.getCreatedAt()));
        conversationsRepository.save(new Conversation(new Conversation.Id(userId, roomIdXZ), message0.getCreatedAt()));
        // other party conversations
        conversationsRepository.save(new Conversation(new Conversation.Id("userY", roomIdXY), message3.getCreatedAt()));
        conversationsRepository.save(new Conversation(new Conversation.Id("userZ", roomIdXZ), message0.getCreatedAt()));


        ResultsPage<ChatMessagePersisted> response = chatService.getConversationsHeaders(userId, pagination);

        assertEquals(2, response.getResults().size());
        assertEquals("Message 3", response.getResults().get(0).getContent()); // most recent on top
        assertEquals("Message 0", response.getResults().get(1).getContent());
        assertNull(response.getNextPageState()); // No more pages
    }

    static Stream<Arguments> conversationHeadersSource() {
        return Stream.of(
                Arguments.of(paginationString(0, 10)),
                Arguments.of((String) null)
        );
    }

    @Test
    void getConversationsHeaders_fourConversationsWithManyMessages_fourConversationsWithLatestMessages() {
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            String roomId;
            if (i % 4 == 0) {
                roomId = "user1:user2";
            } else if (i % 3 == 0) {
                roomId = "user1:user3";
            } else if (i % 2 == 0) {
                roomId = "user1:user4";
            } else {
                roomId = "user1:user5";
            }
            var otherUser = roomId.split(":")[1];
            String senderId;
            String recipientId;
            if (i % (random.nextInt(2) + 1) == 0) {
                senderId = "user1";
                recipientId = otherUser;
            } else {
                senderId = otherUser;
                recipientId = "user1";
            }

            Instant now = Instant.now();
            messageRepository.save(createChatMessage(roomId, "Message " + (i + 1), senderId, recipientId, now));

            var senderConversation = new Conversation(new Conversation.Id(senderId, roomId), now);
            var recipientConversation = new Conversation(new Conversation.Id(recipientId, roomId), now);
            conversationsRepository.saveAll(List.of(senderConversation, recipientConversation));
        }

        ResultsPage<ChatMessagePersisted> response = chatService.getConversationsHeaders("user1", paginationString(0, 10));

        assertEquals(4, response.getResults().size());
        assertThat(response.getNextPageState()).isNull(); // Assuming pagination
        assertThat(response.getResults().get(0).getContent()).isEqualTo("Message 12");
        assertThat(response.getResults().get(1).getContent()).isEqualTo("Message 11");
        assertThat(response.getResults().get(2).getContent()).isEqualTo("Message 10");
        assertThat(response.getResults().get(3).getContent()).isEqualTo("Message 9");
    }

    @Test
    void getConversationsHeaders_manyConversationsWithFewMessages_paginatedUntilAllTheUniqueConversationsRetrieval() {
        Random random = new Random();
        for (int i = 0; i < 30; i++) {
            String roomId = "userA:user" + ((i + 1) % 10);
            var otherUser = roomId.split(":")[1];
            String senderId;
            String recipientId;
            if (i % (random.nextInt(2) + 1) == 0) {
                senderId = "userA";
                recipientId = otherUser;
            } else {
                senderId = otherUser;
                recipientId = "userA";
            }
            Instant now = Instant.now();

            messageRepository.save(createChatMessage(roomId, "Message 1", senderId, recipientId, now));
            var senderConversation1 = new Conversation(new Conversation.Id(senderId, roomId), now);
            var recipientConversation1 = new Conversation(new Conversation.Id(recipientId, roomId), now);
            conversationsRepository.saveAll(List.of(senderConversation1, recipientConversation1));

            Instant secondAhead = now.plusSeconds(1L);
            messageRepository.save(createChatMessage(roomId, "Message 2", recipientId, senderId, secondAhead));
            var senderConversation2 = new Conversation(new Conversation.Id(senderId, roomId), now);
            var recipientConversation2 = new Conversation(new Conversation.Id(recipientId, roomId), now);
            conversationsRepository.saveAll(List.of(senderConversation2, recipientConversation2));
        }
        List<ChatMessagePersisted> messages = new ArrayList<>();
        ResultsPage<ChatMessagePersisted> response = new ResultsPage<>(Collections.emptyList(), paginationString(0, 10));

        do {
            response = chatService.getConversationsHeaders("userA", response.getNextPageState());
            messages.addAll(response.getResults());
        } while (response.getNextPageState() != null);

        assertThat(messages).hasSize(10);
        var first = messages.getFirst();
        var messagesWithoutFirst = new ArrayList<>(messages);
        messagesWithoutFirst.remove(first);
        // first message is the latest
        assertThat(messagesWithoutFirst).allMatch(m -> m.getCreatedAt().compareTo(first.getCreatedAt()) < 0);
        // last page
        assertNull(response.getNextPageState());
    }

    @Test
    void saveMessage_offlineRecipient_savedWithNotification() {
        ChatMessage chatMessage = ChatMessage.basicBuilder()
                .content("Hey, you there?")
                .sender(new ChatUser("user1", "User1"))
                .recipient(new ChatUser("user2", "User2"))
                .build();
        String roomId = CommonUtils.generateRoomId(chatMessage);

        var saved = chatService.saveMessage(chatMessage, roomId, false);

        // Verify message in messages table
        SimpleStatement selectMessage = SimpleStatement.newInstance(
                "SELECT * FROM messages.messages WHERE room_id = ? AND created_at = ? AND profile_id = ?",
                saved.message().getRoomId(), saved.message().getCreatedAt(), saved.message().getProfileId()
        );
        var row = connector.getSession()
                .execute(selectMessage)
                .one();
        Message savedMessage = Message.builder()
                .roomId(row.getString("room_id"))
                .createdAt(row.getInstant("created_at"))
                .profileId(row.getString("profile_id"))
                .username(row.getString("username"))
                .recipientProfileId(row.getString("recipient_profile_id"))
                .recipientUsername(row.getString("recipient_username"))
                .content(row.getString("content"))
                .resourceId(row.getString("resource_id"))
                .threadId(row.getString("thread_id"))
                .editedAt(row.getString("edited_at"))
                .deletedAt(row.getString("deleted_at"))
                .pinned(row.getBoolean("pinned"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .build();
        assertThat(savedMessage).usingRecursiveComparison(new RecursiveComparisonConfiguration())
                .ignoringFields("createdAt", "messageId")
                .isEqualTo(Message.builder()
                        .roomId(roomId)
                        .profileId("user1")
                        .username("User1")
                        .recipientProfileId("user2")
                        .recipientUsername("User2")
                        .content("Hey, you there?")
                        .metadata(Collections.emptyMap())
                        .pinned(false)
                        .build());

        // Verify notification
        MessageNotification savedNotification = messageNotificationRepository.findById(new MessageNotification.Id(
                "user2", LocalDateTime.ofInstant(saved.message().getCreatedAt(), ZoneId.of("UTC")), "user1")
        ).orElseThrow();
        assertThat(savedNotification).usingRecursiveComparison(new RecursiveComparisonConfiguration())
                .ignoringFields("id.createdAt")
                .isEqualTo(MessageNotification.builder()
                        .id(new MessageNotification.Id("user2",
                                null,
                                "user1"))
                        .senderUsername("User1")
                        .content("Hey, you there?")
                        .build());

        // Verify conversations
        assertThat(conversationsRepository.findAll()).containsExactlyInAnyOrder(
                new Conversation(new Conversation.Id("user1", roomId), saved.message().getCreatedAt()),
                new Conversation(new Conversation.Id("user2", roomId), saved.message().getCreatedAt())
        );
    }

    @Test
    void markRead_messageExists_readAtUpdatedAndNotificationDeleted() {
        String roomId = "user1:user2";
        Message message = createChatMessage(roomId, "Hello!", "user1", "user2", Instant.now());
        messageRepository.save(message);
        MessageNotification notification = MessageNotification.builder()
                .id(new MessageNotification.Id("user2",
                        fromInstant(message.getCreatedAt()),
                        "user1"))
                .senderUsername("User1")
                .content("Hello!")
                .build();
        messageNotificationRepository.save(notification);

        MessageKey key = new MessageKey(roomId, List.of(message.getCreatedAt()), "user1");
        Instant readAt = chatService.markRead(key);

        // Verify message readAt updated
        SimpleStatement selectMessage = SimpleStatement.newInstance(
                "SELECT read_at FROM messages.messages WHERE room_id = ? AND created_at = ? AND profile_id = ?",
                roomId, message.getCreatedAt(), "user1"
        );
        var row = connector.getSession().execute(selectMessage).one();
        assertThat(row.getInstant("read_at")).isEqualTo(readAt);

        // Verify notification deleted
        Optional<MessageNotification> notExisting = messageNotificationRepository.findById(new MessageNotification.Id(
                "user2", LocalDateTime.ofInstant(message.getCreatedAt(), ZoneId.of("UTC")), "user1")
        );
        assertThat(notExisting).isEmpty();
    }

    @Test
    void countNotifications_multipleNotifications_correctCount() {
        String userId = "user1";
        LocalDateTime now = LocalDateTime.now();
        MessageNotification notification1 = MessageNotification.builder()
                .id(new MessageNotification.Id(userId,
                        now,
                        "user2"))
                .senderUsername("User2")
                .content("Message 1")
                .build();
        MessageNotification notification2 = MessageNotification.builder()
                .id(new MessageNotification.Id(userId,
                        now.plusSeconds(1L),
                        "user3"))
                .senderUsername("User3")
                .content("Message 2")
                .build();
        MessageNotification otherUserNotification = MessageNotification.builder()
                .id(new MessageNotification.Id("user4",
                        now,
                        "user1"))
                .senderUsername("User1")
                .content("Message 3")
                .build();
        messageNotificationRepository.save(notification1);
        messageNotificationRepository.save(notification2);
        messageNotificationRepository.save(otherUserNotification);

        Long count = chatService.countNotifications(userId);

        assertEquals(2L, count);
    }

    @Test
    void getNotifications_fewMessageNotifications_sortedByCreatedAtDesc() {
        String userId = "user1";
        LocalDateTime now = LocalDateTime.now();
        MessageNotification notification1 = MessageNotification.builder()
                .id(new MessageNotification.Id(userId,
                        now.minusSeconds(10L),
                        "user2"))
                .senderUsername("User2")
                .content("Old message")
                .build();
        MessageNotification notification2 = MessageNotification.builder()
                .id(new MessageNotification.Id(userId,
                        now,
                        "user3"))
                .senderUsername("User3")
                .content("New message")
                .build();
        messageNotificationRepository.save(notification1);
        messageNotificationRepository.save(notification2);

        ResultsPage<MessageNotificationDto> response = chatService.getMessageNotifications(userId, null);

        assertEquals(2, response.getResults().size());
        assertEquals("New message", response.getResults().get(0).getContent());
        assertEquals("Old message", response.getResults().get(1).getContent());
        assertNull(response.getNextPageState());
    }

    @Test
    void getConversation_manyMessages_twoPagedResults() {
        String userId1 = "user1";
        String userId2 = "user2";
        String roomId = CommonUtils.generateRoomId(userId1, userId2);
        List<Message> messages = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            messages.add(createChatMessage(roomId, "Message " + i, userId1, userId2, Instant.now().minusSeconds(15 - i)));
        }
        messages.forEach(messageRepository::save);

        // First page
        ResultsPage<ChatMessagePersisted> response1 = chatService.getConversation(userId1, userId2, null);
        assertEquals(10, response1.getResults().size());
        assertEquals("Message 6", response1.getResults().get(0).getContent());
        assertEquals("Message 15", response1.getResults().get(9).getContent());
        assertThat(response1.getNextPageState()).isNotNull();

        // Second page
        ResultsPage<ChatMessagePersisted> response2 = chatService.getConversation(userId1, userId2, response1.getNextPageState());
        assertEquals(5, response2.getResults().size());
        assertEquals("Message 1", response2.getResults().get(0).getContent());
        assertEquals("Message 5", response2.getResults().get(4).getContent());
        assertNull(response2.getNextPageState());
    }

    public LocalDateTime fromInstant(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }
}

package com.sojka.pomeranian.chat.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.sojka.pomeranian.TestcontainersConfiguration;
import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.db.AstraTestcontainersConnector;
import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.ConversationDto;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.repository.ConversationsRepository;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import com.sojka.pomeranian.lib.dto.ChatUser;
import com.sojka.pomeranian.lib.dto.NotificationDto;
import com.sojka.pomeranian.lib.dto.Pagination;
import com.sojka.pomeranian.security.model.User;
import com.sojka.pomeranian.security.repository.UserRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.sojka.pomeranian.chat.model.Conversation.ContentType.MESSAGE;
import static com.sojka.pomeranian.chat.util.TestUtils.createChatMessage;
import static com.sojka.pomeranian.lib.dto.ConversationFlag.NORMAL;
import static com.sojka.pomeranian.lib.util.CommonUtils.generateRoomId;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toInstant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import({TestcontainersConfiguration.class})
@SpringBootTest
class ChatServiceIntegrationTest {

    @Autowired
    ChatService chatService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    AstraTestcontainersConnector connector;
    @Autowired
    ConversationsRepository conversationsRepository;

    CqlSession session;

    UUID user1Id = UUID.randomUUID();
    UUID user2Id = UUID.randomUUID();
    UUID user3Id = UUID.randomUUID();
    UUID user4Id = UUID.randomUUID();
    UUID user5Id = UUID.randomUUID();
    UUID userX = UUID.randomUUID();
    UUID userY = UUID.randomUUID();
    UUID userZ = UUID.randomUUID();
    String roomIdXY = generateRoomId(userX, userY);
    String roomIdXZ = generateRoomId(userX, userZ);

    @BeforeEach
    void setUp() {
        session = connector.connect();
        session.execute("TRUNCATE messages.messages");
        userRepository.deleteAll();
        conversationsRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void saveMessage_message_savedWithBothConversations() {
        String content = "Hello, World!";
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        ChatMessage chatMessage = ChatMessage.basicBuilder()
                .content(content)
                .sender(new ChatUser(user1Id, user1Id + "-username", null))
                .recipient(new ChatUser(user2Id, user2Id + "-username", null))
                .build();
        String roomId = CommonUtils.generateRoomId(chatMessage);

        var saved = chatService.saveMessage(chatMessage, roomId, false);

        // Verify message in messages table
        SimpleStatement selectMessage = SimpleStatement.newInstance(
                "SELECT * FROM messages.messages WHERE room_id = ? AND created_at = ? AND profile_id = ?",
                roomId, toInstant(saved.message().getCreatedAt()), saved.message().getSender().id()
        );
        var row = connector.getSession()
                .execute(selectMessage)
                .one();
        Message saveResult = Message.builder()
                .roomId(row.getString("room_id"))
                .createdAt(row.getInstant("created_at"))
                .profileId(row.getUuid("profile_id"))
                .username(row.getString("username"))
                .recipientProfileId(row.getUuid("recipient_profile_id"))
                .recipientUsername(row.getString("recipient_username"))
                .content(row.getString("content"))
                .resourceId(row.getUuid("resource_id"))
                .editedAt(row.getString("edited_at"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .build();
        assertThat(saveResult).usingRecursiveComparison(new RecursiveComparisonConfiguration())
                .ignoringFields("createdAt", "messageId")
                .isEqualTo(Message.builder()
                        .roomId(roomId)
                        .profileId(user1Id)
                        .username(user1Id + "-username")
                        .recipientProfileId(user2Id)
                        .recipientUsername(user2Id + "-username")
                        .content(content)
                        .metadata(Collections.emptyMap())
                        .build());

        assertThat(conversationsRepository.findAll()).containsExactly(
                createReadConversation(new Conversation.Id(user1Id, user2Id), saveResult.getCreatedAt(), content),
                createUnreadConversation(new Conversation.Id(user2Id, user1Id), saveResult.getCreatedAt(), content)
        );
    }

    @Test
    void getConversation_fewMessages_sameMessagesInDescOrder() {
        Message message1 = createChatMessage(roomIdXY, "Message 1", userX, userY, Instant.now().minusSeconds(10));
        Message message2 = createChatMessage(roomIdXY, "Message 2", userX, userY, Instant.now().minusSeconds(5));
        Message message3 = createChatMessage(roomIdXY, "Message 3", userY, userX, Instant.now());
        messageRepository.save(message1);
        messageRepository.save(message2);
        messageRepository.save(message3);

        var response = chatService.getConversation(userX, userY, null);

        assertEquals(3, response.getResults().size());
        assertEquals("Message 1", response.getResults().get(0).getContent()); // Sorted by created_at DESC
        assertEquals("Message 2", response.getResults().get(1).getContent());
        assertEquals("Message 3", response.getResults().get(2).getContent());

        assertNull(response.getNextPageState()); // No more pages
    }

    @ParameterizedTest
    @MethodSource("conversationHeadersSource")
    void getConversationsHeaders_twoConversationsWithFewMessagesEach_twoConversationHeaders(Pagination pagination) {
        Message message3 = createChatMessage(roomIdXY, "Message 3", userY, userX, Instant.now());
        Message message2 = createChatMessage(roomIdXY, "Message 2", userX, userY, Instant.now().minusSeconds(5));
        Message message1 = createChatMessage(roomIdXY, "Message 1", userX, userZ, Instant.now().minusSeconds(10));
        Message message0 = createChatMessage(roomIdXZ, "Message 0", userZ, userX, Instant.now().minusSeconds(15));
        Message messageNotInTheScope = createChatMessage("userY:userZ", "Other users message", userZ, userY, Instant.now().minusSeconds(15));
        messageRepository.save(messageNotInTheScope);
        messageRepository.save(message0);
        messageRepository.save(message1);
        messageRepository.save(message2);
        messageRepository.save(message3);
        // user conversations
        conversationsRepository.save(createReadConversation(new Conversation.Id(userX, userY), message2.getCreatedAt(), "Message 2"));
        conversationsRepository.save(createReadConversation(new Conversation.Id(userX, userZ), message1.getCreatedAt(), "Message 1"));
        // other party conversations
        conversationsRepository.save(createReadConversation(new Conversation.Id(userY, userX), message3.getCreatedAt(), "Message 3"));
        conversationsRepository.save(createReadConversation(new Conversation.Id(userZ, userX), message0.getCreatedAt(), "Message 0"));

        ResultsPage<ConversationDto> response = chatService.getConversations(userX, NORMAL, pagination);

        assertEquals(2, response.getResults().size());
        assertEquals("Message 2", response.getResults().get(0).getContent()); // most recent on top
        assertEquals("Message 1", response.getResults().get(1).getContent());
        assertNull(response.getNextPageState()); // No more pages
    }

    static Stream<Arguments> conversationHeadersSource() {
        return Stream.of(
                Arguments.of(new Pagination(0, 10)),
                Arguments.of((Pagination) null)
        );
    }

    @Test
    void getConversationsHeaders_fourConversationsWithManyMessages_fourConversationsWithLatestMessages() {
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            String roomId;
            if (i % 4 == 0) {
                roomId = user1Id + ":" + user2Id;
            } else if (i % 3 == 0) {
                roomId = user1Id + ":" + user3Id;
            } else if (i % 2 == 0) {
                roomId = user1Id + ":" + user4Id;
            } else {
                roomId = user1Id + ":" + user5Id;
            }
            var otherUser = UUID.fromString(roomId.split(":")[1]);
            UUID senderId;
            UUID recipientId;
            if (i % (random.nextInt(2) + 1) == 0) {
                senderId = user1Id;
                recipientId = otherUser;
            } else {
                senderId = otherUser;
                recipientId = user1Id;
            }
            String content = "Message " + (i + 1);

            Instant now = Instant.now();
            messageRepository.save(createChatMessage(roomId, content, senderId, recipientId, now));

            var senderConversation = createReadConversation(new Conversation.Id(senderId, recipientId), now, content);
            var recipientConversation = createUnreadConversation(new Conversation.Id(recipientId, senderId), now, content);
            conversationsRepository.saveAll(List.of(senderConversation, recipientConversation));
        }

        ResultsPage<ConversationDto> response = chatService.getConversations(user1Id, NORMAL, new Pagination(0, 10));

        assertEquals(4, response.getResults().size());
        assertNull(response.getNextPageState()); // Assuming pagination
        assertThat(response.getResults().get(0).getContent()).isEqualTo("Message 12");
        assertThat(response.getResults().get(1).getContent()).isEqualTo("Message 11");
        assertThat(response.getResults().get(2).getContent()).isEqualTo("Message 10");
        assertThat(response.getResults().get(3).getContent()).isEqualTo("Message 9");
    }

//    @Test
//    void getConversationsHeaders_manyConversationsWithFewMessages_paginatedUntilAllTheUniqueConversationsRetrieval() {
//        Random random = new Random();
//        for (int i = 0; i < 30; i++) {
//            String roomId = "userA:user" + ((i + 1) % 10);
//            var otherUser = roomId.split(":")[1];
//            String senderId;
//            String recipientId;
//            if (i % (random.nextInt(2) + 1) == 0) {
//                senderId = "userA";
//                recipientId = otherUser;
//            } else {
//                senderId = otherUser;
//                recipientId = "userA";
//            }
//            Instant now = Instant.now();
//
//            messageRepository.save(createChatMessage(roomId, "Message 1", senderId, recipientId, now));
//            var senderConversation1 = new Conversation(new Conversation.Id(senderId, roomId), null, now);
//            var recipientConversation1 = new Conversation(new Conversation.Id(recipientId, roomId), null, now);
//            conversationsRepository.saveAll(List.of(senderConversation1, recipientConversation1));
//
//            Instant secondAhead = now.plusSeconds(1L);
//            messageRepository.save(createChatMessage(roomId, "Message 2", recipientId, senderId, secondAhead));
//            var senderConversation2 = new Conversation(new Conversation.Id(senderId, roomId), null, now);
//            var recipientConversation2 = new Conversation(new Conversation.Id(recipientId, roomId), null, now);
//            conversationsRepository.saveAll(List.of(senderConversation2, recipientConversation2));
//        }
//        List<ChatMessagePersisted> messages = new ArrayList<>();
//        ResultsPage<ChatMessagePersisted> response = new ResultsPage<>(Collections.emptyList(), toEncodedString(0, 10));
//
//        do {
//            response = chatService.getConversations("userA", response.getNextPageState());
//            messages.addAll(response.getResults());
//        } while (response.getNextPageState() != null);
//
//        assertThat(messages).hasSize(10);
//        var first = messages.getFirst();
//        var messagesWithoutFirst = new ArrayList<>(messages);
//        messagesWithoutFirst.remove(first);
//        // first message is the latest
//        assertThat(messagesWithoutFirst).allMatch(m -> Instant.parse(m.getCreatedAt())
//                .compareTo(Instant.parse(first.getCreatedAt())) < 0);
//        // last page
//        assertNull(response.getNextPageState());
//    }

    @Test
    void saveMessage_offlineRecipient_savedWithConversation() {
        String content = "Hey, you there?";
        ChatMessage chatMessage = ChatMessage.basicBuilder()
                .content(content)
                .sender(new ChatUser(user1Id, user1Id + "-username", null))
                .recipient(new ChatUser(user2Id, user2Id + "-username", null))
                .build();
        String roomId = CommonUtils.generateRoomId(chatMessage);

        var saved = chatService.saveMessage(chatMessage, roomId, false);

        // Verify message in messages table
        SimpleStatement selectMessage = SimpleStatement.newInstance(
                "SELECT * FROM messages.messages WHERE room_id = ? AND created_at = ? AND profile_id = ?",
                roomId, toInstant(saved.message().getCreatedAt()), saved.message().getSender().id()
        );
        var row = connector.getSession()
                .execute(selectMessage)
                .one();
        Message savedMessage = Message.builder()
                .roomId(row.getString("room_id"))
                .createdAt(row.getInstant("created_at"))
                .profileId(row.getUuid("profile_id"))
                .username(row.getString("username"))
                .recipientProfileId(row.getUuid("recipient_profile_id"))
                .recipientUsername(row.getString("recipient_username"))
                .content(row.getString("content"))
                .resourceId(row.getUuid("resource_id"))
                .editedAt(row.getString("edited_at"))
                .metadata(row.getMap("metadata", String.class, String.class))
                .build();
        assertThat(savedMessage).usingRecursiveComparison(new RecursiveComparisonConfiguration())
                .ignoringFields("createdAt", "messageId")
                .isEqualTo(Message.builder()
                        .roomId(roomId)
                        .profileId(user1Id)
                        .username(user1Id + "-username")
                        .recipientProfileId(user2Id)
                        .recipientUsername(user2Id + "-username")
                        .content(content)
                        .metadata(Collections.emptyMap())
                        .build());
        // Verify conversations
        assertThat(conversationsRepository.findAll()).containsExactlyInAnyOrder(
                createReadConversation(new Conversation.Id(user1Id, user2Id), toInstant(saved.message().getCreatedAt()), content),
                createUnreadConversation(new Conversation.Id(user2Id, user1Id), toInstant(saved.message().getCreatedAt()), content)
        );
    }

    @Test
    void markRead_messageExists_readAtUpdatedAndConversationUpdated() {
        String roomId = user1Id + ":" + user2Id;
        Message message = createChatMessage(roomId, "Hello!", user1Id, user2Id, Instant.now());
        messageRepository.save(message);
        Conversation conversation = createUnreadConversation(new Conversation.Id(user2Id, user1Id), message.getCreatedAt(), "Hello!");
        conversationsRepository.save(conversation);

        MessageKey key = new MessageKey(roomId, List.of(message.getCreatedAt()), user1Id);
        Instant readAt = chatService.markRead(key);

        // Verify message readAt updated
        var row = connector.getSession().execute(selectMessage(roomId, message.getCreatedAt(), user1Id)).one();
        assertThat(row.getInstant("read_at")).isEqualTo(readAt);
        // Verify notification deleted
        assertThat(conversationsRepository.findById(new Conversation.Id(user2Id, user1Id)).get().getUnreadCount()).isZero();
    }

    @Test
    void markRead_messageNotExists_notUpdated() {
        String roomId = user1Id + ":" + user2Id;
        Message message = createChatMessage(roomId, "Hello!", user1Id, user2Id, Instant.now());
        messageRepository.save(message);
        Conversation conversation = createUnreadConversation(new Conversation.Id(user2Id, user1Id), message.getCreatedAt(), "Hello!");
        conversationsRepository.save(conversation);

        MessageKey key = new MessageKey(roomId, List.of(message.getCreatedAt()), user2Id);
        chatService.markRead(key);

        // Verify message readAt not updated and empty message not created
        var existingMessage = connector.getSession().execute(selectMessage(roomId, message.getCreatedAt(), user1Id)).one();
        assertThat(existingMessage.getInstant("read_at")).isNull();
        var updatedKeyMessage = connector.getSession().execute(selectMessage(roomId, message.getCreatedAt(), user2Id)).one();
        assertThat(updatedKeyMessage).isNull();
        // Verify conversation count not updated
        assertThat(conversationsRepository.findById(new Conversation.Id(user2Id, user1Id)).get().getUnreadCount()).isOne();
    }

    @Test
    void markRead_messagesPartiallyExists_partiallyUpdated() {
        String roomId = user1Id + ":" + user2Id;
        Message message = createChatMessage(roomId, "Hello!", user1Id, user2Id, Instant.now());
        messageRepository.save(message);
        Conversation conversation = createUnreadConversation(new Conversation.Id(user2Id, user1Id), message.getCreatedAt(), "Hello!");
        conversationsRepository.save(conversation);

        Instant notExistingKey = Instant.now();
        MessageKey key = new MessageKey(roomId, List.of(message.getCreatedAt(), notExistingKey), user1Id);
        chatService.markRead(key);

        // Verify message readAt not updated and empty message not created
        var existingMessage = connector.getSession().execute(selectMessage(roomId, message.getCreatedAt(), user1Id)).one();
        assertThat(existingMessage.getInstant("read_at")).isNull();
        var updatedKeyMessage = connector.getSession().execute(selectMessage(roomId, notExistingKey, user1Id)).one();
        assertThat(updatedKeyMessage).isNull();
        // Verify notification deleted
        assertThat(conversationsRepository.findById(new Conversation.Id(user2Id, user1Id)).get().getUnreadCount()).isZero();
    }

    @Test
    void countNotifications_multipleNotifications_correctCount() {
        Instant now = Instant.now();
        Conversation conversation1 = createUnreadConversation(new Conversation.Id(userX, user2Id), now, "Message 1");
        Conversation conversation2 = createUnreadConversation(new Conversation.Id(userX, user3Id), now.plusSeconds(1L), "Message 2");
        Conversation otherUserNotification = createUnreadConversation(new Conversation.Id(user4Id, user1Id), now, "Message 3");
        conversationsRepository.saveAll(List.of(conversation1, conversation2, otherUserNotification));

        Long count = chatService.countNotifications(userX);

        assertEquals(2L, count);
    }

    @Test
    void getNotifications_fewMessageNotifications_sortedByCreatedAtDesc() {
        Instant now = Instant.now();
        Conversation conversation1 = createUnreadConversation(new Conversation.Id(userX, user2Id), now.minusSeconds(1L), "Old message");
        Conversation conversation2 = createUnreadConversation(new Conversation.Id(userX, user3Id), now, "New message");
        conversationsRepository.saveAll(List.of(conversation1, conversation2));

        ResultsPage<NotificationDto> response = chatService.getMessageNotifications(userX, null);

        assertEquals(2, response.getResults().size());
        assertEquals("New message", response.getResults().get(0).getContent());
        assertEquals("Old message", response.getResults().get(1).getContent());
        assertNull(response.getNextPageState());
    }

    @Test
    void getConversation_manyMessages_twoPagedResults() {
        List<Message> messages = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            messages.add(createChatMessage(roomIdXY, "Message " + i, userX, userY, Instant.now().minusSeconds(15 - i)));
        }
        messages.forEach(messageRepository::save);

        // First page
        ResultsPage<ChatMessagePersisted> response1 = chatService.getConversation(userX, userY, null);
        assertEquals(20, response1.getResults().size());
        assertEquals("Message 6", response1.getResults().get(0).getContent());
        assertEquals("Message 25", response1.getResults().get(19).getContent());
        assertThat(response1.getNextPageState()).isNotNull();

        // Second page
        ResultsPage<ChatMessagePersisted> response2 = chatService.getConversation(userX, userY, response1.getNextPageState());
        assertEquals(5, response2.getResults().size());
        assertEquals("Message 1", response2.getResults().get(0).getContent());
        assertEquals("Message 5", response2.getResults().get(4).getContent());
        assertNull(response2.getNextPageState());
    }

    @Test
    void deleteUserInactiveRooms_noRooms_emptySet() {
        Set<String> removed = chatService.deleteUserInactiveRooms(user1Id);

        assertThat(removed).isEmpty();
    }

    @Test
    void deleteUserInactiveRooms_roomsWithActiveUsers_noDeletion() {
        User user2 = User.builder().id(user2Id).build();
        User user3 = User.builder().id(user3Id).build();
        userRepository.save(user2);
        userRepository.save(user3);

        String room12 = generateRoomId(user1Id, user2Id);
        String room13 = generateRoomId(user1Id, user3Id);

        chatService.saveMessage(MessageMapper.toDto(createChatMessage(room12, "hi2", user2Id, user1Id, Instant.now())), room12, false);
        chatService.saveMessage(MessageMapper.toDto(createChatMessage(room13, "hi", user1Id, user3Id, Instant.now())), room13, false);

        Set<String> removed = chatService.deleteUserInactiveRooms(user1Id);

        assertThat(removed).isEmpty();

        assertThat(messageRepository.findByRoomId(room12, null, 10).getResults()).isNotEmpty();
        assertThat(messageRepository.findByRoomId(room13, null, 10).getResults()).isNotEmpty();
    }

    @Test
    void deleteUserInactiveMessages_roomsWithInactiveUsers_roomsDeleted() {
        String room12 = generateRoomId(user1Id, user2Id);
        String room13 = generateRoomId(user1Id, user3Id);

        chatService.saveMessage(MessageMapper.toDto(createChatMessage(room12, "hi1", user1Id, user2Id, Instant.now().minusSeconds(10))), room12, false);
        chatService.saveMessage(MessageMapper.toDto(createChatMessage(room12, "hi2", user2Id, user1Id, Instant.now())), room12, false);
        chatService.saveMessage(MessageMapper.toDto(createChatMessage(room13, "hi", user1Id, user3Id, Instant.now())), room13, false);

        Set<String> removed = chatService.deleteUserInactiveRooms(user1Id);

        assertThat(removed).containsExactlyInAnyOrder(room12, room13);

        assertThat(messageRepository.findByRoomId(room12, null, 10).getResults()).isEmpty();
        assertThat(messageRepository.findByRoomId(room13, null, 10).getResults()).isEmpty();
    }

    @Test
    void deleteUserInactiveMessages_mixedActiveInactive_selectiveDeletion() {
        User user2 = User.builder().id(user2Id).build();
        userRepository.save(user2);

        String room12 = generateRoomId(user1Id, user2Id);
        String room13 = generateRoomId(user1Id, user3Id);
        String room14 = generateRoomId(user1Id, user4Id);

        chatService.saveMessage(MessageMapper.toDto(createChatMessage(room12, "hi", user1Id, user2Id, Instant.now())), room12, false);
        chatService.saveMessage(MessageMapper.toDto(createChatMessage(room13, "hi", user1Id, user3Id, Instant.now())), room13, false);
        chatService.saveMessage(MessageMapper.toDto(createChatMessage(room14, "hi", user1Id, user4Id, Instant.now())), room14, false);

        Set<String> removed = chatService.deleteUserInactiveRooms(user1Id);

        assertThat(removed).containsExactlyInAnyOrder(room13, room14);

        assertThat(messageRepository.findByRoomId(room12, null, 10).getResults()).isNotEmpty();
        assertThat(messageRepository.findByRoomId(room13, null, 10).getResults()).isEmpty();
        assertThat(messageRepository.findByRoomId(room14, null, 10).getResults()).isEmpty();
    }

    @Test
    void getRoomUnreadMessagesCount() {
        var conversation1 = createConversation(new Conversation.Id(userX, userY), Instant.now(), "dummy", 2);
        var conversation2 = createUnreadConversation(new Conversation.Id(userX, userZ), Instant.now(), "dummy");
        conversationsRepository.saveAll(List.of(conversation1, conversation2));

        assertThat(chatService.getRoomUnreadMessagesCount(userX, generateRoomId(userX, userZ))).isEqualTo(1L);
    }


    SimpleStatement selectMessage(String roomId, Instant createdAt, UUID userId) {
        return SimpleStatement.newInstance(
                "SELECT read_at FROM messages.messages WHERE room_id = ? AND created_at = ? AND profile_id = ?",
                roomId, createdAt, userId
        );
    }

    Conversation createUnreadConversation(Conversation.Id id, Instant lastMessageAt, String content) {
        return createConversation(id, lastMessageAt, content, 1);
    }

    Conversation createReadConversation(Conversation.Id id, Instant lastMessageAt, String content) {
        return createConversation(id, lastMessageAt, content, 0);
    }

    Conversation createConversation(Conversation.Id id, Instant lastMessageAt, String content, int unreadCount) {
        return Conversation.builder()
                .id(id)
                .lastMessageAt(lastMessageAt)
                .content(content)
                .unreadCount(unreadCount)
                .flag(NORMAL)
                .contentType(MESSAGE)
                .build();
    }

}

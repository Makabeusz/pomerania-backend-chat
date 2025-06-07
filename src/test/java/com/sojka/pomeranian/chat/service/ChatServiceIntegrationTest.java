package com.sojka.pomeranian.chat.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.sojka.pomeranian.TestcontainersConfiguration;
import com.sojka.pomeranian.chat.db.AstraTestcontainersConnector;
import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.chat.dto.ChatMessageResponse;
import com.sojka.pomeranian.chat.dto.ChatUser;
import com.sojka.pomeranian.chat.dto.MessagePageResponse;
import com.sojka.pomeranian.chat.dto.MessageType;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.repository.ConversationsRepository;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.util.CommonUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    CqlSession session;

    @BeforeEach
    void setUp() {
        session = connector.connect();
        session.execute("TRUNCATE messages.messages");
        conversationsRepository.deleteAll();
    }

    @Test
    void saveMessage_message_savedWithBothConversations() {
        ChatMessage chatMessage = ChatMessage.builder()
                .content("Hello, World!")
                .type(MessageType.CHAT)
                .sender(new ChatUser("user1", "User1"))
                .recipient(new ChatUser("user2", "User2"))
                .build();

        Message savedMessage = chatService.saveMessage(chatMessage, false);

        // Verify message in messages table
        SimpleStatement selectMessage = SimpleStatement.newInstance(
                "SELECT * FROM messages.messages WHERE room_id = ? AND created_at = ? AND message_id = ?",
                savedMessage.getRoomId(), savedMessage.getCreatedAt(), savedMessage.getProfileId()
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
                .messageType(row.getString("message_type"))
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
                        .roomId(CommonUtils.generateRoomId(chatMessage))
                        .profileId("user1")
                        .username("User1")
                        .recipientProfileId("user2")
                        .recipientUsername("User2")
                        .content("Hello, World!")
                        .messageType("CHAT")
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

        MessagePageResponse response = chatService.getConversation(userId1, userId2, null);

        assertEquals(3, response.getMessages().size());
        assertEquals("Message 1", response.getMessages().get(0).getContent()); // Sorted by created_at DESC
        assertEquals("Message 2", response.getMessages().get(1).getContent());
        assertEquals("Message 3", response.getMessages().get(2).getContent());

        assertNull(response.getNextPageState()); // No more pages
    }

    @Test
    void getConversationsHeaders_twoConversationsWithFewMessagesEach_twoConversationHeaders() {
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


        MessagePageResponse response = chatService.getConversationsHeaders(userId, paginationString(0, 10));

        assertEquals(2, response.getMessages().size());
        assertEquals("Message 3", response.getMessages().get(0).getContent()); // newest on top
        assertEquals("Message 0", response.getMessages().get(1).getContent());
        assertNull(response.getNextPageState()); // No more pages
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

        MessagePageResponse response = chatService.getConversationsHeaders("user1", paginationString(0, 10));

        assertEquals(4, response.getMessages().size());
        assertThat(response.getNextPageState()).isNull(); // Assuming pagination
        assertThat(response.getMessages().get(0).getContent()).isEqualTo("Message 12");
        assertThat(response.getMessages().get(1).getContent()).isEqualTo("Message 11");
        assertThat(response.getMessages().get(2).getContent()).isEqualTo("Message 10");
        assertThat(response.getMessages().get(3).getContent()).isEqualTo("Message 9");
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
        List<ChatMessageResponse> messages = new ArrayList<>();
        MessagePageResponse response = new MessagePageResponse(Collections.emptyList(), paginationString(0, 10));

        do {
            response = chatService.getConversationsHeaders("userA", response.getNextPageState());
            messages.addAll(response.getMessages());
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

}

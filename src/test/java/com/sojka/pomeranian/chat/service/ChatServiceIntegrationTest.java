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
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.sojka.pomeranian.chat.util.TestUtils.createChatMessage;
import static com.sojka.pomeranian.chat.util.TestUtils.timestampComparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ChatServiceIntegrationTest {

    @Autowired
    ChatService chatService;
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    AstraTestcontainersConnector connector;

    CqlSession session;

    @BeforeEach
    void setUp() {
        session = connector.connect();
        session.execute("TRUNCATE messages.messages");
        session.execute("TRUNCATE messages.conversations");
    }

    @Test
    void saveMessage_message_savedWithBothConversations() {
        ChatMessage chatMessage = ChatMessage.builder()
                .content("Hello, World!")
                .type(MessageType.CHAT)
                .sender(new ChatUser("user1", "User1"))
                .recipient(new ChatUser("user2", "User2"))
                .build();

        Message savedMessage = chatService.saveMessage(chatMessage);

        // Verify message in messages table
        SimpleStatement selectMessage = SimpleStatement.newInstance(
                "SELECT content FROM messages.messages WHERE room_id = ? AND created_at = ? AND message_id = ?",
                savedMessage.getRoomId(), savedMessage.getCreatedAt(), savedMessage.getMessageId()
        );
        String retrievedContent = connector.getSession()
                .execute(selectMessage)
                .one()
                .getString("content");
        assertEquals("Hello, World!", retrievedContent);

        // Verify conversations table for both users
        SimpleStatement selectConversation = SimpleStatement.newInstance(
                "SELECT last_message_at FROM messages.conversations WHERE user_id = ?",
                savedMessage.getProfileId()
        );
        var lastMessageCreatedAt = LocalDateTime.ofInstant(Objects.requireNonNull(
                        connector.getSession()
                                .execute(selectConversation)
                                .one()
                                .getInstant("last_message_at")),
                ZoneId.systemDefault());

        assertThat(lastMessageCreatedAt)
                .usingComparator(timestampComparator())
                .isEqualTo(LocalDateTime.ofInstant(savedMessage.getCreatedAt(), ZoneId.systemDefault()));

        selectConversation = SimpleStatement.newInstance(
                "SELECT last_message_at FROM messages.conversations WHERE user_id = ?",
                savedMessage.getRecipientProfileId()
        );
        lastMessageCreatedAt = LocalDateTime.ofInstant(Objects.requireNonNull(
                        connector.getSession()
                                .execute(selectConversation)
                                .one()
                                .getInstant("last_message_at")),
                ZoneId.systemDefault());

        assertThat(lastMessageCreatedAt)
                .usingComparator(timestampComparator())
                .isEqualTo(LocalDateTime.ofInstant(savedMessage.getCreatedAt(), ZoneId.systemDefault()));
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

        MessagePageResponse response = chatService.getConversationsHeaders(userId, null);

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
            String sender;
            String recipient;
            if (i % (random.nextInt(2) + 1) == 0) {
                sender = "user1";
                recipient = otherUser;
            } else {
                sender = otherUser;
                recipient = "user1";
            }
            messageRepository.save(createChatMessage(roomId, "Message " + (i + 1), sender, recipient, Instant.now()));
        }

        MessagePageResponse response = chatService.getConversationsHeaders("user1", null);

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
            String sender;
            String recipient;
            if (i % (random.nextInt(2) + 1) == 0) {
                sender = "userA";
                recipient = otherUser;
            } else {
                sender = otherUser;
                recipient = "userA";
            }
            messageRepository.save(createChatMessage(roomId, "Message 1", sender, recipient, Instant.now().plusSeconds(0L)));
            messageRepository.save(createChatMessage(roomId, "Message 2", recipient, sender, Instant.now().plusSeconds(1L)));
        }
        List<ChatMessageResponse> messages = new ArrayList<>();
        MessagePageResponse response = new MessagePageResponse(Collections.emptyList(), null);

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

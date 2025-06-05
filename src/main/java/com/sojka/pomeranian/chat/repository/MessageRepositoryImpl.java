package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.BatchableStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.sojka.pomeranian.chat.db.AstraConnector;
import com.sojka.pomeranian.chat.dto.MessagePage;
import com.sojka.pomeranian.chat.exception.AstraException;
import com.sojka.pomeranian.chat.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder.DESC;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {

    private static final String MESSAGES_TABLE = "messages";
    private static final String CONVERSATIONS_TABLE = "conversations";
    private static final String CONVERSATION_INDEX_TABLE = "conversations_index";
    private static final String KEYSPACE = "messages";

    private final AstraConnector connector;

    @Override
    public MessagePage findByRoomId(String roomId, String pageState, int pageSize) {
        Select select = QueryBuilder.selectFrom(MESSAGES_TABLE)
                .all()
                .whereColumn("room_id").isEqualTo(literal(roomId));

        log.info(String.format("Querying room_id=%s, pageState=%s", roomId, pageState));

        ByteBuffer pagingStateBuffer = null;
        if (pageState != null) {
            try {
                pagingStateBuffer = ByteBuffer.wrap(Base64.getUrlDecoder().decode(pageState));
            } catch (IllegalArgumentException e) {
                throw new AstraException("Invalid pageState for room_id %s: %s".formatted(roomId, e.getMessage()), e);
            }
        }

        SimpleStatement statement = select.build()
                .setPageSize(pageSize)
                .setPagingState(pagingStateBuffer);

        try {
            CqlSession session = connector.getSession();
            ResultSet resultSet = session.execute(statement);
            List<Message> messages = new ArrayList<>();
            int rowCount = 0;

            for (Row row : resultSet) {
                Message message = new Message();
                message.setRoomId(row.getString("room_id"));
                message.setCreatedAt(row.getInstant("created_at"));
                message.setMessageId(row.getString("message_id"));
                message.setProfileId(row.getString("profile_id"));
                message.setUsername(row.getString("username"));
                message.setRecipientUsername(row.getString("recipient_username"));
                message.setRecipientProfileId(row.getString("recipient_profile_id"));
                message.setContent(row.getString("content"));
                message.setMessageType(row.getString("message_type"));
                message.setResourceId(row.getString("resource_id"));
                message.setThreadId(row.getString("thread_id"));
                message.setEditedAt(row.getString("edited_at"));
                message.setDeletedAt(row.getString("deleted_at"));
                message.setPinned(row.getBoolean("pinned"));
                message.setMetadata(row.getMap("metadata", String.class, String.class));
                messages.add(message);

                if (++rowCount >= pageSize) {
                    break;
                }
            }

            log.info(String.format("Fetched %d messages for room_id=%s", messages.size(), roomId));

            ByteBuffer pagingState = resultSet.getExecutionInfo().getPagingState();
            String nextPageState = pagingState != null ? Base64.getUrlEncoder().withoutPadding().encodeToString(pagingState.array()) : null;

            return new MessagePage(messages, nextPageState);
        } catch (IllegalStateException e) {
            log.error(String.format("CqlSession not initialized for room_id %s: %s", roomId, e.getMessage()), e);
            throw new AstraException("Cassandra session not initialized", e);
        } catch (Exception e) {
            log.error(String.format("Failed to find messages for room_id %s: %s", roomId, e.getMessage()), e);
            throw new AstraException("Unexpected issue", e);
        }
    }

    /**
     * Saves the message and updates conversation rows for both users.
     */
    @Override
    public Message save(Message message) {
        try {
            // Insert into messages.messages
            RegularInsert messageInsert = QueryBuilder.insertInto(KEYSPACE, MESSAGES_TABLE)
                    .value("room_id", literal(message.getRoomId()))
                    .value("created_at", literal(message.getCreatedAt()))
                    .value("message_id", literal(message.getMessageId()))
                    .value("profile_id", literal(message.getProfileId()))
                    .value("username", literal(message.getUsername()))
                    .value("recipient_profile_id", literal(message.getRecipientProfileId()))
                    .value("recipient_username", literal(message.getRecipientUsername()))
                    .value("content", literal(message.getContent()))
                    .value("message_type", literal(message.getMessageType()))
                    .value("resource_id", literal(message.getResourceId()))
                    .value("thread_id", literal(message.getThreadId()))
                    .value("edited_at", literal(message.getEditedAt()))
                    .value("deleted_at", literal(message.getDeletedAt()))
                    .value("pinned", literal(message.getPinned()))
                    .value("metadata", literal(message.getMetadata()));

            // Sender: Query index for old last_message_at
            SimpleStatement senderIndexSelect = QueryBuilder.selectFrom(KEYSPACE, CONVERSATION_INDEX_TABLE)
                    .column("last_message_at")
                    .whereColumn("user_id").isEqualTo(literal(message.getProfileId()))
                    .whereColumn("room_id").isEqualTo(literal(message.getRoomId()))
                    .build();
            Row senderIndexRow = connector.getSession().execute(senderIndexSelect).one();
            Instant senderOldLastMessageAt = senderIndexRow != null ? senderIndexRow.getInstant("last_message_at") : null;

            // Sender: Batch index update, old conversation delete, new conversation insert
            var senderBatch = new ArrayList<BatchableStatement<?>>();
            senderBatch.add(QueryBuilder.insertInto(KEYSPACE, CONVERSATION_INDEX_TABLE)
                    .value("user_id", literal(message.getProfileId()))
                    .value("room_id", literal(message.getRoomId()))
                    .value("last_message_at", literal(message.getCreatedAt()))
                    .build());
            if (senderOldLastMessageAt != null) {
                senderBatch.add(QueryBuilder.deleteFrom(KEYSPACE, CONVERSATIONS_TABLE)
                        .whereColumn("user_id").isEqualTo(literal(message.getProfileId()))
                        .whereColumn("last_message_at").isEqualTo(literal(senderOldLastMessageAt))
                        .whereColumn("room_id").isEqualTo(literal(message.getRoomId()))
                        .build());
            }
            senderBatch.add(QueryBuilder.insertInto(KEYSPACE, CONVERSATIONS_TABLE)
                    .value("user_id", literal(message.getProfileId()))
                    .value("last_message_at", literal(message.getCreatedAt()))
                    .value("room_id", literal(message.getRoomId()))
                    .value("other_user_id", literal(message.getRecipientProfileId()))
                    .value("other_username", literal(message.getRecipientUsername()))
                    .value("last_message_content", literal(message.getContent()))
                    .build());

            // Recipient: Query index for old last_message_at
            SimpleStatement recipientIndexSelect = QueryBuilder.selectFrom(KEYSPACE, CONVERSATION_INDEX_TABLE)
                    .column("last_message_at")
                    .whereColumn("user_id").isEqualTo(literal(message.getRecipientProfileId()))
                    .whereColumn("room_id").isEqualTo(literal(message.getRoomId()))
                    .build();
            Row recipientIndexRow = connector.getSession().execute(recipientIndexSelect).one();
            Instant recipientOldLastMessageAt = recipientIndexRow != null ? recipientIndexRow.getInstant("last_message_at") : null;

            // Recipient: Batch index update, old conversation delete, new conversation insert
            var recipientBatch = new ArrayList<BatchableStatement<?>>();
            recipientBatch.add(QueryBuilder.insertInto(KEYSPACE, CONVERSATION_INDEX_TABLE)
                    .value("user_id", literal(message.getRecipientProfileId()))
                    .value("room_id", literal(message.getRoomId()))
                    .value("last_message_at", literal(message.getCreatedAt()))
                    .build());
            if (recipientOldLastMessageAt != null) {
                recipientBatch.add(QueryBuilder.deleteFrom(KEYSPACE, CONVERSATIONS_TABLE)
                        .whereColumn("user_id").isEqualTo(literal(message.getRecipientProfileId()))
                        .whereColumn("last_message_at").isEqualTo(literal(recipientOldLastMessageAt))
                        .whereColumn("room_id").isEqualTo(literal(message.getRoomId()))
                        .build());
            }
            recipientBatch.add(QueryBuilder.insertInto(KEYSPACE, CONVERSATIONS_TABLE)
                    .value("user_id", literal(message.getRecipientProfileId()))
                    .value("last_message_at", literal(message.getCreatedAt()))
                    .value("room_id", literal(message.getRoomId()))
                    .value("other_user_id", literal(message.getProfileId()))
                    .value("other_username", literal(message.getUsername()))
                    .value("last_message_content", literal(message.getContent()))
                    .build());

            // Combine all operations
            BatchStatement batch = BatchStatement.builder(BatchType.UNLOGGED)
                    .addStatement(messageInsert.build())
                    .addStatements(senderBatch)
                    .addStatements(recipientBatch)
                    .build();

            CqlSession session = connector.getSession();
            session.execute(batch);
            log.info("Saved message and updated conversations: room_id={}, message_id={}", message.getRoomId(), message.getMessageId());
            return message;
        } catch (Exception e) {
            log.error("Failed to save message and conversations: {}", e.getMessage(), e);
            throw new AstraException("Failed to save message for room_id=" + message.getRoomId(), e);
        }
    }

    @Override
    public MessagePage findConversationsHeaders(String userId, String pageState, int pageSize) {
        var select = QueryBuilder.selectFrom(KEYSPACE, CONVERSATIONS_TABLE)
                .all()
                .whereColumn("user_id").isEqualTo(literal(userId))
                .orderBy("last_message_at", DESC);

        ByteBuffer pagingStateBuffer = null;
        if (pageState != null) {
            try {
                pagingStateBuffer = ByteBuffer.wrap(Base64.getUrlDecoder().decode(pageState));
            } catch (IllegalArgumentException e) {
                log.error(String.format("Invalid pageState for conversations user_id=%s: %s", userId, e.getMessage()), e);
                throw new AstraException("Invalid conversation headers pageState for user_id %s: %s".formatted(userId, e.getMessage()), e);
            }
        }

        SimpleStatement statement = select.build()
                .setPageSize(pageSize)
                .setPagingState(pagingStateBuffer);

        try {
            CqlSession session = connector.getSession();
            ResultSet resultSet = session.execute(statement);
            List<Message> conversations = new ArrayList<>();
            String nextPageState = null;
            int rowCount = 0;

            for (Row row : resultSet) {
                Message conversation = new Message();
                conversation.setRoomId(row.getString("room_id"));
                conversation.setProfileId(userId);
                conversation.setUsername("User" + userId); // Adjust based on your domain logic
                conversation.setRecipientProfileId(row.getString("other_user_id"));
                conversation.setRecipientUsername(row.getString("other_username"));
                conversation.setContent(row.getString("last_message_content"));
                conversation.setCreatedAt(row.getInstant("last_message_at"));
                conversation.setMessageType("CHAT"); // Adjust as needed
                conversations.add(conversation);

                if (++rowCount >= pageSize) {
                    ByteBuffer pagingState = resultSet.getExecutionInfo().getPagingState();
                    nextPageState = pagingState != null ? Base64.getUrlEncoder().withoutPadding().encodeToString(pagingState.array()) : null;
                    break;
                }
            }

            return new MessagePage(conversations, nextPageState);
        } catch (Exception e) {
            log.error("Failed to fetch conversations for user_id {}: {}", userId, e.getMessage(), e);
            throw new AstraException("Failed to fetch conversations", e);
        }
    }
}
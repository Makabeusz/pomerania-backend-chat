package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.sojka.pomeranian.chat.db.AstraConnector;
import com.sojka.pomeranian.chat.dto.MessagePage;
import com.sojka.pomeranian.chat.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {

    private static final String MESSAGES_TABLE = "messages";
    private static final String CONVERSATIONS_TABLE = "conversations";
    private static final String KEYSPACE = "messages";

    private final int pageSize = 10;

    private final AstraConnector connector;

    @Override
    public MessagePage findByRoomId(String roomId, String pageState) {
        Select select = QueryBuilder.selectFrom(MESSAGES_TABLE)
                .all()
                .whereColumn("room_id").isEqualTo(literal(roomId));

        log.info(String.format("Querying room_id=%s, pageState=%s", roomId, pageState));

        ByteBuffer pagingStateBuffer = null;
        if (pageState != null) {
            try {
                pagingStateBuffer = ByteBuffer.wrap(Base64.getUrlDecoder().decode(pageState));
            } catch (IllegalArgumentException e) {
                log.error(String.format("Invalid pageState for room_id %s: %s", roomId, e.getMessage()), e);
                return new MessagePage(List.of(), null);
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
            throw new RuntimeException("Cassandra session not initialized", e);
        } catch (Exception e) {
            log.error(String.format("Failed to find messages for room_id %s: %s", roomId, e.getMessage()), e);
            return new MessagePage(List.of(), null);
        }
    }

    /**
     * Saves the message and update conversations for recipient and sender.
     */
    @Override
    public Message save(Message message) {
        // Build insert for messages table
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

        // Build upserts for conversations table
        RegularInsert senderConversationInsert = QueryBuilder.insertInto(KEYSPACE, CONVERSATIONS_TABLE)
                .value("room_id", literal(message.getRoomId()))
                .value("last_message_created_at", literal(message.getCreatedAt()))
                .value("user_id", literal(message.getProfileId()));

        RegularInsert recipientConversationInsert = QueryBuilder.insertInto(KEYSPACE, CONVERSATIONS_TABLE)
                .value("room_id", literal(message.getRoomId()))
                .value("last_message_created_at", literal(message.getCreatedAt()))
                .value("user_id", literal(message.getRecipientProfileId()));

        // Combine into an unlogged batch
        BatchStatement batch = BatchStatement.builder(BatchType.UNLOGGED)
                .addStatement(messageInsert.build())
                .addStatement(senderConversationInsert.build())
                .addStatement(recipientConversationInsert.build())
                .build();

        try {
            CqlSession session = connector.getSession();
            session.execute(batch);

            log.info(String.format("Saved message and updated conversations: room_id=%s, message_id=%s",
                    message.getRoomId(), message.getMessageId()));
            return message;
        } catch (IllegalStateException e) {
            log.error(String.format("CqlSession not initialized for save: %s", e.getMessage()), e);
            throw new RuntimeException("Cassandra session not initialized", e);
        } catch (Exception e) {
            log.error(String.format("Failed to save message and conversations: %s", e.getMessage()), e);
            throw new RuntimeException(String.format("Failed to save message for room_id %s", message.getRoomId()), e);
        }
    }

    @Override
    public MessagePage findConversationsHeaders(String userId, String pageState) {
        try {
            CqlSession session = connector.getSession();
            // Step 1: Fetch room_ids for the user from user_conversations
            Select roomIdSelect = QueryBuilder.selectFrom(CONVERSATIONS_TABLE)
                    .column("room_id")
                    .whereColumn("user_id").isEqualTo(literal(userId));

            ByteBuffer pagingStateBuffer = null;
            if (pageState != null) {
                try {
                    pagingStateBuffer = ByteBuffer.wrap(Base64.getUrlDecoder().decode(pageState));
                } catch (IllegalArgumentException e) {
                    log.error(String.format("Invalid pageState for user_id %s: %s", userId, e.getMessage()), e);
                    return new MessagePage(List.of(), null);
                }
            }

            SimpleStatement roomIdStatement = roomIdSelect.build()
                    .setPageSize(pageSize)
                    .setPagingState(pagingStateBuffer);

            ResultSet roomIdResultSet = session.execute(roomIdStatement);
            List<String> roomIds = new ArrayList<>();
            for (Row row : roomIdResultSet) {
                roomIds.add(row.getString("room_id"));
            }

            if (roomIds.isEmpty()) {
                log.info(String.format("No conversations found for user_id=%s", userId));
                return new MessagePage(List.of(), null);
            }

            // Step 2: Fetch latest message for each room_id
            // todo: duplication - extract common part with findByRoomId
            List<Message> messages = new ArrayList<>();
            for (String roomId : roomIds) {
                Select select2 = QueryBuilder.selectFrom(MESSAGES_TABLE)
                        .all()
                        .whereColumn("room_id").isEqualTo(literal(roomId));

                SimpleStatement statement = select2.build()
                        .setPageSize(pageSize)
                        .setPagingState(pagingStateBuffer);

                ResultSet resultSet = session.execute(statement);

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

                    break;
                }


            }

            log.info(String.format("Fetched %d conversation messages for user_id=%s", messages.size(), userId));

            ByteBuffer pagingState = roomIdResultSet.getExecutionInfo().getPagingState();
            String nextPageState = pagingState != null ? Base64.getUrlEncoder().withoutPadding().encodeToString(pagingState.array()) : null;

            return new MessagePage(messages, nextPageState);
        } catch (IllegalStateException e) {
            log.error(String.format("CqlSession not initialized for user_id %s: %s", userId, e.getMessage()), e);
            throw new RuntimeException("Cassandra session not initialized", e);
        } catch (Exception e) {
            log.error(String.format("Failed to find conversations for user_id %s: %s", userId, e.getMessage()), e);
            return new MessagePage(List.of(), null);
        }
    }
}
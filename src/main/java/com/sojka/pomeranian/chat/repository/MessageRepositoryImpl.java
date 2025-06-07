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
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessagePage;
import com.sojka.pomeranian.chat.exception.AstraException;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {

    private static final String MESSAGES_TABLE = "messages";
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
                message.setReadAt(row.getInstant("read_at"));
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
            Objects.requireNonNull(message.getContent());
            Objects.requireNonNull(message.getUsername());

            // Insert into messages.messages
            RegularInsert messageInsert = QueryBuilder.insertInto(KEYSPACE, MESSAGES_TABLE)
                    .value("room_id", literal(message.getRoomId()))
                    .value("created_at", literal(message.getCreatedAt()))
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
                    .value("read_at", literal(message.getReadAt()))
                    .value("metadata", literal(message.getMetadata()));

            CqlSession session = connector.getSession();
            session.execute(messageInsert.build());
            log.info("Saved message and updated conversations: {}", new MessageKey(message));
            return message;
        } catch (Exception e) {
            log.error("Failed to save message and conversations: {}", e.getMessage(), e);
            throw new AstraException("Failed to save message for room_id=" + message.getRoomId(), e);
        }
    }

    @Override
    public Instant markRead(List<MessageKey> key) {
        try {
            Instant readTime = CommonUtils.getCurrentInstant();
            var update = key.stream()
                    .map(k -> QueryBuilder.insertInto(KEYSPACE, MESSAGES_TABLE)
                            .value("room_id", literal(k.roomId()))
                            .value("created_at", literal(k.createdAt()))
                            .value("profile_id", literal(k.profileId()))
                            .value("read_at", literal(readTime))
                            .build())
                    .toList();

            BatchStatement statement = BatchStatement.builder(BatchType.LOGGED)
                    .addStatements(new ArrayList<>(update))
                    .build();

            CqlSession session = connector.getSession();
            session.execute(statement);

            log.info("Marked message as read: {}, read_at={}", key, readTime);

            return readTime;
        } catch (Exception e) {
            log.error("Failed to mark message as read: {}", key, e);
            throw new AstraException("Failed mark message as read: " + key, e);
        }
    }
}
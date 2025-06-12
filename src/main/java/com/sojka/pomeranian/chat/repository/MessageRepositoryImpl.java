package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.sojka.pomeranian.chat.db.AstraConnector;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.ResultsPage;
import com.sojka.pomeranian.chat.exception.AstraException;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import static com.sojka.pomeranian.chat.util.Constants.ASTRA_KEYSPACE;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl extends AstraRepository implements MessageRepository {

    private static final String MESSAGES_TABLE = "messages";

    private final AstraConnector connector;

    @Override
    public ResultsPage<Message> findByRoomId(String roomId, String pageState, int pageSize) {
        var select = QueryBuilder.selectFrom(MESSAGES_TABLE)
                .all()
                .whereColumn("room_id").isEqualTo(literal(roomId));

        log.info(String.format("Querying room_id=%s, pageState=%s", roomId, pageState));

        ByteBuffer pagingStateBuffer = decodePageState(pageState);

        var statement = select.build()
                .setPageSize(pageSize)
                .setPagingState(pagingStateBuffer);

        try {
            var session = connector.getSession();
            var resultSet = session.execute(statement);

            var result = resultsPage(resultSet, pageSize, MessageMapper::fromAstraRow);

            log.info(String.format("Fetched %d messages for room_id=%s", result.getResults().size(), roomId));

            return result;
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
            var messageInsert = QueryBuilder.insertInto(ASTRA_KEYSPACE, MESSAGES_TABLE)
                    .value("room_id", literal(message.getRoomId()))
                    .value("created_at", literal(message.getCreatedAt()))
                    .value("profile_id", literal(message.getProfileId()))
                    .value("username", literal(message.getUsername()))
                    .value("recipient_profile_id", literal(message.getRecipientProfileId()))
                    .value("recipient_username", literal(message.getRecipientUsername()))
                    .value("content", literal(message.getContent()))
                    .value("resource_id", literal(message.getResourceId()))
                    .value("thread_id", literal(message.getThreadId()))
                    .value("edited_at", literal(message.getEditedAt()))
                    .value("deleted_at", literal(message.getDeletedAt()))
                    .value("pinned", literal(message.getPinned()))
                    .value("read_at", literal(message.getReadAt()))
                    .value("metadata", literal(message.getMetadata()));

            var session = connector.getSession();
            session.execute(messageInsert.build());
            log.info("Saved message and updated conversations: {}", new MessageKey(message));
            return message;
        } catch (Exception e) {
            log.error("Failed to save message and conversations: {}", e.getMessage(), e);
            throw new AstraException("Failed to save message for room_id=" + message.getRoomId(), e);
        }
    }

    @Override
    public Instant markRead(MessageKey key) {
        try {
            var readTime = CommonUtils.getCurrentInstant();
            var update = key.createdAt().stream()
                    .map(k -> QueryBuilder.insertInto(ASTRA_KEYSPACE, MESSAGES_TABLE)
                            .value("room_id", literal(key.roomId()))
                            .value("created_at", literal(k))
                            .value("profile_id", literal(key.profileId()))
                            .value("read_at", literal(readTime))
                            .build())
                    .toList();

            var statement = BatchStatement.builder(BatchType.LOGGED)
                    .addStatements(new ArrayList<>(update))
                    .build();

            var session = connector.getSession();
            session.execute(statement);

            log.info("Marked message as read: {}, read_at={}", key, readTime);

            return readTime;
        } catch (Exception e) {
            log.error("Failed to mark message as read: {}", key, e);
            throw new AstraException("Failed mark message as read: " + key, e);
        }
    }

    @Override
    public void delete(Message message) {
        throw new RuntimeException("Not implemented");
    }
}
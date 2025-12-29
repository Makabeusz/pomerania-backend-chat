package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.SimpleStatementBuilder;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.sojka.pomeranian.astra.connection.Connector;
import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.astra.repository.AstraPageableRepository;
import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.model.Message;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import static com.sojka.pomeranian.chat.util.Constants.MESSAGES_KEYSPACE;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.getCurrentInstant;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toInstant;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MessageRepository extends AstraPageableRepository {

    private static final String MESSAGES_TABLE = "messages";
    private static final String deleteAllRoomMessages = "DELETE FROM %s.%s WHERE room_id = ?"
            .formatted(MESSAGES_KEYSPACE, MESSAGES_TABLE);
    private static final String DELETE_MESSAGE = "DELETE FROM %s.%s WHERE room_id = ? and created_at = ? and profile_id = ?"
            .formatted(MESSAGES_KEYSPACE, MESSAGES_TABLE);
    private static final String FIND_MESSAGE = "SELECT * FROM %s.%s WHERE room_id = ? and created_at = ? and profile_id = ?"
            .formatted(MESSAGES_KEYSPACE, MESSAGES_TABLE);

    private final Connector connector;

    @Override
    public Logger getLogger() {
        return log;
    }

    public ResultsPage<Message> findByRoomId(String roomId, String pageState, int pageSize) {
        var id = new RoomIdState(roomId, pageState, pageSize);
        log.trace("findByRoomId input: {}", id);
        return execute(() -> {
            var select = QueryBuilder.selectFrom(MESSAGES_KEYSPACE, MESSAGES_TABLE)
                    .all()
                    .whereColumn("room_id")
                    .isEqualTo(literal(roomId));

            ByteBuffer pagingStateBuffer = decodePageState(pageState);
            var statement = select.build().setPageSize(pageSize).setPagingState(pagingStateBuffer);

            var resultSet = connector.getSession().execute(statement);

            var result = resultsPage(resultSet, pageSize, MessageMapper::fromAstraRow);

            log.trace(String.format("Fetched %d messages for room_id=%s", result.getResults().size(), roomId));

            return result;
        }, "findByRoomId", id);
    }

    public record RoomIdState(String roomId, String pageState, int pageSize) {
    }

    /**
     * Saves the message and updates conversation rows for both users.
     */
    public Message save(@Valid Message message) {
        log.trace("save input: {}", message);

        if (!StringUtils.hasText(message.getContent()) && message.getResourceId() == null) {
            throw new IllegalArgumentException("No content or resource in the message: " + message);
        }

        return execute(() -> {
            // Insert into messages.messages todo: refactor to plain text query
            var messageInsert = QueryBuilder.insertInto(MESSAGES_KEYSPACE, MESSAGES_TABLE)
                    .value("room_id", literal(message.getRoomId()))
                    .value("created_at", literal(message.getCreatedAt()))
                    .value("profile_id", literal(message.getProfileId()))
                    .value("username", literal(message.getUsername()))
                    .value("recipient_profile_id", literal(message.getRecipientProfileId()))
                    .value("recipient_username", literal(message.getRecipientUsername()))
                    .value("content", literal(message.getContent()))
                    .value("resource_id", literal(message.getResourceId()))
                    .value("resource_type", literal(message.getResourceType()))
                    .value("edited_at", literal(message.getEditedAt()))
                    .value("read_at", literal(message.getReadAt()))
                    .value("metadata", literal(message.getMetadata()));

            var session = connector.getSession();
            session.execute(messageInsert.build());
            log.trace("Saved message: {}", new MessageKey(message));
            return message;
        }, "save", message);
    }

    public Instant markRead(MessageKey key) {
        log.trace("markRead input: {}", key);
        return execute(() -> {
            var readTime = getCurrentInstant();
            var update = key.createdAt().stream()
                    // todo: to plain text query
                    .map(k -> QueryBuilder.insertInto(MESSAGES_KEYSPACE, MESSAGES_TABLE)
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

            log.trace("Marked message as read: {}, read_at={}", key, readTime);

            return readTime;
        }, "markRead", key);
    }

    public boolean deleteRoom(String roomId) {
        log.trace("deleteRoom input: roomId={}", roomId);
        return execute(() -> {
            var statement = new SimpleStatementBuilder(deleteAllRoomMessages).addPositionalValue(roomId).build();
            connector.getSession().execute(statement);
            return true;
        }, "purgeMessages", roomId);
    }

    public void delete(String roomId, String createdAt, UUID profileId) {
        log.trace("delete input: roomId={}, createdAt={}, profileId={}", roomId, createdAt, profileId);
        execute(() -> {
            var statement = new SimpleStatementBuilder(DELETE_MESSAGE)
                    .addPositionalValue(roomId)
                    .addPositionalValue(toInstant(createdAt))
                    .addPositionalValue(profileId)
                    .build();
            connector.getSession().execute(statement);
            return true;
        }, "delete", List.of(roomId, createdAt, profileId));
    }

    public Optional<Message> findById(String roomId, String createdAt, UUID profileId) {
        var id = new IdState(roomId, createdAt, profileId);
        log.trace("findById input: {}", id);
        return execute(() -> {
            var statement = new SimpleStatementBuilder(FIND_MESSAGE)
                    .addPositionalValue(roomId)
                    .addPositionalValue(toInstant(createdAt))
                    .addPositionalValue(profileId)
                    .build();

            var resultSet = connector.getSession().execute(statement);

            return Optional.ofNullable(resultSet.one()).map(MessageMapper::fromAstraRow);
        }, "findById", id);
    }

    public record IdState(String roomId, String createdAt, UUID profileId) {
    }

    /**
     * Saves the message and updates conversation rows for both users.
     */
    public Message update(@Valid Message message) {
        log.trace("update input: {}", message);

        return execute(() -> {
            // Insert into messages.messages todo: refactor to plain text query
            var messageUpdate = QueryBuilder.update(MESSAGES_KEYSPACE, MESSAGES_TABLE)
                    .setColumn("username", literal(message.getUsername()))
                    .setColumn("recipient_profile_id", literal(message.getRecipientProfileId()))
                    .setColumn("recipient_username", literal(message.getRecipientUsername()))
                    .setColumn("content", literal(message.getContent()))
                    .setColumn("resource_id", literal(message.getResourceId()))
                    .setColumn("resource_type", literal(message.getResourceType()))
                    .setColumn("edited_at", literal(message.getEditedAt()))
                    //.setColumn("read_at", literal(message.getReadAt()))
                    .setColumn("metadata", literal(message.getMetadata()))
                    .whereColumn("room_id").isEqualTo(literal(message.getRoomId()))
                    .whereColumn("created_at").isEqualTo(literal(message.getCreatedAt()))
                    .whereColumn("profile_id").isEqualTo(literal(message.getProfileId()));

            var session = connector.getSession();
            session.execute(messageUpdate.build());
            log.trace("Updated message: {}", new MessageKey(message));
            return message;
        }, "update", message);
    }

}
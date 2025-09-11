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
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import static com.sojka.pomeranian.chat.util.Constants.MESSAGES_KEYSPACE;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.getCurrentInstant;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MessageRepository extends AstraPageableRepository {

    private static final String MESSAGES_TABLE = "messages";
    private static final String deleteAllRoomMessages = "DELETE FROM messages WHERE ROOM_ID = ?";

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
                    .value("thread_id", literal(message.getThreadId()))
                    .value("edited_at", literal(message.getEditedAt()))
                    .value("deleted_at", literal(message.getDeletedAt()))
                    .value("pinned", literal(message.getPinned()))
                    .value("read_at", literal(message.getReadAt()))
                    .value("metadata", literal(message.getMetadata()));

            var session = connector.getSession();
            session.execute(messageInsert.build());
            log.trace("Saved message and updated conversations: {}", new MessageKey(message));
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

}
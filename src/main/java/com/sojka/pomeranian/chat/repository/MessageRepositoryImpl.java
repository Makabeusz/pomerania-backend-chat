package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.sojka.pomeranian.chat.db.AstraConnector;
import com.sojka.pomeranian.chat.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {

    private static final String MESSAGES_TABLE = "messages";
    private static final String KEYSPACE = "messages";

    int pageSize = 20;

    private final AstraConnector connector;

    @Override
    public Flux<Message> findByRoomId(String roomId, String pageState) {
        Select select = QueryBuilder.selectFrom(MESSAGES_TABLE)
                .all()
                .whereColumn("room_id").isEqualTo(literal(roomId));

        SimpleStatement statement = select.build()
                .setPageSize(pageSize)
                .setPagingState(pageState != null ? ByteBuffer.wrap(java.util.Base64.getDecoder().decode(pageState)) : null);

        try {
            var session = connector.getSession();
            return Flux.from(session.executeReactive(statement))
                    .map(row -> {
                        Message message = new Message();
                        message.setRoomId(row.getString("room_id"));
                        message.setCreatedAt(row.getInstant("created_at"));
                        message.setMessageId(row.getString("message_id"));
                        message.setProfileId(row.getString("profile_id"));
                        message.setUsername(row.getString("username"));
                        message.setContent(row.getString("content"));
                        message.setMessageType(row.getString("message_type"));
                        message.setResourceId(row.getString("resource_id"));
                        message.setThreadId(row.getString("thread_id"));
                        message.setEditedAt(row.getString("edited_at"));
                        message.setDeletedAt(row.getString("deleted_at"));
                        message.setPinned(row.getBoolean("pinned"));
                        message.setMetadata(row.getMap("metadata", String.class, String.class));
                        return message;
                    })
                    .doOnError(e -> log.error("Failed to find messages for room_id {}: {}", roomId, e.getMessage()))
                    .onErrorResume(e -> Flux.empty());
        } catch (IllegalStateException e) {
            log.error("CqlSession not initialized for room_id {}: {}", roomId, e.getMessage());
            return Flux.error(new RuntimeException("Cassandra session not initialized", e));
        }
    }

    @Override
    public Mono<Message> save(Message message) {
        RegularInsert insert = QueryBuilder.insertInto(KEYSPACE, MESSAGES_TABLE)
                .value("room_id", literal(message.getRoomId()))
                .value("created_at", literal(message.getCreatedAt()))
                .value("message_id", literal(message.getMessageId()))
                .value("profile_id", literal(message.getProfileId()))
                .value("username", literal(message.getUsername()))
                .value("content", literal(message.getContent()))
                .value("message_type", literal(message.getMessageType()))
                .value("resource_id", literal(message.getResourceId()))
                .value("thread_id", literal(message.getThreadId()))
                .value("edited_at", literal(message.getEditedAt()))
                .value("deleted_at", literal(message.getDeletedAt()))
                .value("pinned", literal(message.getPinned()))
                .value("metadata", literal(message.getMetadata()));

        SimpleStatement statement = insert.build();

        try {
            CqlSession session = connector.getSession();
            return Mono.fromCompletionStage(session.executeAsync(statement))
                    .then(Mono.just(message))
                    .doOnSuccess(m -> log.info("Saved message: room_id={}, message_id={}", m.getRoomId(), m.getMessageId()))
                    .doOnError(e -> log.error("Failed to save message: {}", e.getMessage()))
                    .onErrorMap(e -> new RuntimeException("Failed to save message for room_id " + message.getRoomId(), e));
        } catch (IllegalStateException e) {
            log.error("CqlSession not initialized for save: {}", e.getMessage());
            return Mono.error(new RuntimeException("Cassandra session not initialized", e));
        }
    }
}
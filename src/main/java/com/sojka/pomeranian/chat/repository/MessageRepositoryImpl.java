package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.CqlSession;
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
    private static final String KEYSPACE = "messages";

    private final int pageSize = 2;

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
                return new MessagePage(List.of(), null); // Reset to first page
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
                if (rowCount >= pageSize) {
                    break;
                }
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
                messages.add(message);
                rowCount++;
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

    @Override
    public Message save(Message message) {
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
            session.execute(statement);
            log.info(String.format("Saved message: room_id=%s, message_id=%s", message.getRoomId(), message.getMessageId()));
            return message;
        } catch (IllegalStateException e) {
            log.error(String.format("CqlSession not initialized for save: %s", e.getMessage()), e);
            throw new RuntimeException("Cassandra session not initialized", e);
        } catch (Exception e) {
            log.error(String.format("Failed to save message: %s", e.getMessage()), e);
            throw new RuntimeException(String.format("Failed to save message for room_id %s", message.getRoomId()), e);
        }
    }
}
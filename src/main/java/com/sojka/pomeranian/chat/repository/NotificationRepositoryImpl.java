package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.sojka.pomeranian.chat.db.AstraConnector;
import com.sojka.pomeranian.chat.dto.ResultsPage;
import com.sojka.pomeranian.chat.exception.AstraException;
import com.sojka.pomeranian.chat.model.Notification;
import com.sojka.pomeranian.chat.util.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import static com.sojka.pomeranian.chat.util.Constants.ASTRA_KEYSPACE;

@Slf4j
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl extends AstraRepository implements NotificationRepository {

    private static final String NOTIFICATION_TABLE = "notifications";

    private final AstraConnector connector;

    private static final String DELETE_ONE = """
            DELETE FROM %s.%s \
            WHERE profile_id = ? \
            AND created_at = ? \
            AND sender_id = ?
            """.formatted(ASTRA_KEYSPACE, NOTIFICATION_TABLE);
    private static final String COUNT_BY_PROFILE_ID =
            "SELECT COUNT(*) FROM %s.%s WHERE profile_id = ?".formatted(ASTRA_KEYSPACE, NOTIFICATION_TABLE);
    private static final String SELECT_ALL_BY_PROFILE_ID =
            "SELECT * FROM %s.%s WHERE profile_id = ?".formatted(ASTRA_KEYSPACE, NOTIFICATION_TABLE);

    @Override
    public Notification save(Notification notification) {
        try {
            // Insert into messages.messages
            var notificationInsert = QueryBuilder.insertInto(ASTRA_KEYSPACE, NOTIFICATION_TABLE)
                    .value("profile_id", literal(notification.getProfileId()))
                    .value("created_at", literal(notification.getCreatedAt()))
                    .value("sender_id", literal(notification.getSenderId()))
                    .value("sender_username", literal(notification.getSenderUsername()))
                    .value("content", literal(notification.getContent()));

            var session = connector.getSession();
            session.execute(notificationInsert.build());
            log.info("Saved notification: profile_id={}, created_at={}, sender_id={}", notification.getProfileId(), notification.getCreatedAt(), notification.getSenderId());
            return notification;
        } catch (Exception e) {
            log.error("Failed to save notification: {}", e.getMessage(), e);
            throw new AstraException("Failed to save notification: profile_id=%s, created_at=%s, sender_id=%s"
                    .formatted(notification.getProfileId(), notification.getCreatedAt(), notification.getSenderId()), e);
        }
    }

    @Override
    public void delete(Notification notification) {
        try {
            var statement = SimpleStatement.builder(DELETE_ONE)
                    .addPositionalValues(List.of(
                            notification.getProfileId(), notification.getCreatedAt(), notification.getSenderId()))
                    .build();

            var session = connector.getSession();
            session.execute(statement);
            log.info("Deleted notification: profile_id={}, created_at={}, sender_id={}", notification.getProfileId(), notification.getCreatedAt(), notification.getSenderId());
        } catch (Exception e) {
            log.error("Failed to delete notification: {}", e.getMessage(), e);
            throw new AstraException("Failed to delete notification: profile_id=%s, created_at=%s, sender_id=%s"
                    .formatted(notification.getProfileId(), notification.getCreatedAt(), notification.getSenderId()), e);
        }
    }

    @Override
    public void deleteAllByPrimaryKeys(String profileId, List<Instant> createdAt, String senderId) {
        try {
            var deletes = createdAt.stream()
                    .map(ts -> SimpleStatement.builder(DELETE_ONE)
                            .addPositionalValues(List.of(profileId, ts, senderId))
                            .build())
                    .toList();

            var batchDeleteStatement = BatchStatement.builder(BatchType.LOGGED)
                    .addStatements(new ArrayList<>(deletes))
                    .build();

            var session = connector.getSession();
            session.execute(batchDeleteStatement);
            log.info("Deleted notifications: profile_id={}, sender_id={}, created_at={},", profileId, senderId, createdAt);
        } catch (Exception e) {
            log.error("Failed to delete notification: {}", e.getMessage(), e);
            throw new AstraException("Failed to delete notification: profile_id=%s, sender_id=%s, created_at=%s"
                    .formatted(profileId, senderId, createdAt), e);
        }
    }

    @Override
    public Optional<Long> countByProfileId(String profileId) {
        try {
            var statement = SimpleStatement.builder(COUNT_BY_PROFILE_ID).addPositionalValue(profileId).build();

            log.info("Debug, count query: {}", statement.getQuery());

            var session = connector.getSession();
            var row = session.execute(statement).one();

            Objects.requireNonNull(row);
            Optional<Long> count = Optional.of(row.getLong("count"));

            log.info("Fetched notifications count={}, profile_id={}", count.orElse(-1L), profileId);
            return count;
        } catch (Exception e) {
            throw new AstraException("Failed to fetch notifications count: profile_id=%s".formatted(profileId), e);
        }
    }

    @Override
    public ResultsPage<Notification> findByProfileId(String profileId, String pageState, int pageSize) {

        var select = SimpleStatement.builder(SELECT_ALL_BY_PROFILE_ID).addPositionalValue(profileId);

        ByteBuffer pagingStateBuffer = decodePageState(pageState);

        var statement = select.build()
                .setPageSize(pageSize)
                .setPagingState(pagingStateBuffer);

        log.info("Querying a notifications for profile_id={}", profileId);

        try {
            var session = connector.getSession();
            var resultSet = session.execute(statement);

            var result = resultsPage(resultSet, pageSize, NotificationMapper::fromAstraRow);

            log.info("Fetched {} notifications, profile_id={}", result.getResults().size(), profileId);

            return result;
        } catch (IllegalStateException e) {
            log.error(String.format("CqlSession not initialized for profile_id %s: %s", profileId, e.getMessage()), e);
            throw new AstraException("Cassandra session not initialized", e);
        } catch (Exception e) {
            log.error(String.format("Failed to find messages for profile_id %s: %s", profileId, e.getMessage()), e);
            throw new AstraException("Unexpected issue", e);
        }
    }
}

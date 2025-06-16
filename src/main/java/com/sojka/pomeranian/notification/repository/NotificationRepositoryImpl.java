package com.sojka.pomeranian.notification.repository;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.sojka.pomeranian.astra.connection.Connector;
import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.astra.exception.AstraException;
import com.sojka.pomeranian.astra.repository.AstraRepository;
import com.sojka.pomeranian.notification.model.Notification;
import com.sojka.pomeranian.notification.util.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.sojka.pomeranian.chat.util.Constants.NOTIFICATIONS_KEYSPACE;

@Slf4j
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl extends AstraRepository implements NotificationRepository {

    private static final String NOTIFICATIONS_TABLE = "notifications";
    private static final String INSERT = """
            INSERT INTO %s.%s ( \
            profile_id, created_at, type, read_at, related_id, content, metadata \
            ) VALUES (?, ?, ?, ?, ?, ?, ?)""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);
    private static final String USING_TTL = " USING TTL %s";
    private static final String SELECT_BY_PRIMARY_KEY = """
            SELECT * FROM %s.%s \
            WHERE profile_id = ? \
            AND created_at = ? \
            AND type = ?""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);
    private static final String SELECT_BY_PROFILE_ID = """
            SELECT * FROM %s.%s \
            WHERE profile_id = ?""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);

    private final Connector connector;

    @Override
    public Notification save(Notification notification, int ttl) {
        return execute(() -> {
            String dml = INSERT + (ttl > 0 ? USING_TTL.formatted(ttl) : "");
            var statement = SimpleStatement.builder(dml)
                    .addPositionalValues(notification.getProfileId(), notification.getCreatedAt(),
                            notification.getType().name(), notification.getReadAt(), notification.getRelatedId(),
                            notification.getContent(), notification.getMetadata())
                    .build();

            var session = connector.getSession();
            ResultSet executed = session.execute(statement);

            executed.all().forEach(r -> System.out.println(r.getFormattedContents()));

            return notification;
        }, "save", notification);
    }

    @Override
    public Notification save(Notification notification) {
        return save(notification, -1);
    }

    @Override
    public Optional<Notification> findBy(String profileId, Instant createdAt, Notification.Type type) {
        return execute(() -> {
            var statement = SimpleStatement.builder(SELECT_BY_PRIMARY_KEY)
                    .addPositionalValues(profileId, createdAt, type)
                    .build();

            var session = connector.getSession();
            Row row = session.execute(statement).one();

            return Optional.ofNullable(NotificationMapper.fromAstraRow(row));
        }, "findBy", List.of(profileId, createdAt, type));
    }

    @Override
    public ResultsPage<Notification> findAllBy(String profileId, String pageState, int pageSize) {
        return execute(() -> {
            ByteBuffer pagingStateBuffer = decodePageState(pageState);

            var statement = SimpleStatement.builder(SELECT_BY_PROFILE_ID)
                    .addPositionalValues(profileId)
                    .setPageSize(pageSize)
                    .setPagingState(pagingStateBuffer)
                    .build();

            var session = connector.getSession();
            ResultSet resultSet = session.execute(statement);

            return resultsPage(resultSet, pageSize, NotificationMapper::fromAstraRow);
        }, "findAllBy", profileId);
    }

    <T> T execute(Callable<T> callable, String methodName, Object id) {
        try {
            return callable.call();
        } catch (IllegalStateException e) {
            log.error("CqlSession not initialized for {}, error: {}", id, e.getMessage(), e);
            throw new AstraException("Cassandra session not initialized, id=" + id, e);
        } catch (Exception e) {
            log.error("Failed to execute {}, for: {}", methodName, id);
            throw new AstraException("Failed to execute %s, for: %s".formatted(methodName, id), e);
        }
    }
}

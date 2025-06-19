package com.sojka.pomeranian.notification.repository;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.sojka.pomeranian.astra.connection.Connector;
import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.astra.exception.AstraException;
import com.sojka.pomeranian.astra.repository.AstraRepository;
import com.sojka.pomeranian.notification.model.NotificationType;
import com.sojka.pomeranian.notification.model.ReadNotification;
import com.sojka.pomeranian.notification.util.ReadNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.sojka.pomeranian.chat.util.Constants.NOTIFICATIONS_KEYSPACE;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReadNotificationRepositoryImpl extends AstraRepository<ReadNotification> implements ReadNotificationRepository {

    private static final String READ_NOTIFICATIONS_TABLE = "read_notifications";

    private static final String INSERT = """
            INSERT INTO %s.%s ( \
            profile_id, created_at, type, read_at, related_id, content, metadata \
            ) VALUES (?, ?, ?, ?, ?, ?, ?)""".formatted(NOTIFICATIONS_KEYSPACE, READ_NOTIFICATIONS_TABLE);
    private static final String USING_TTL = " USING TTL %s";

    private final Connector connector;

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public ReadNotification save(ReadNotification notification, int ttl) {
        validateTtl(ttl);
        return execute(() -> {
            String dml = INSERT + USING_TTL.formatted(ttl);
            var statement = SimpleStatement.builder(dml)
                    .addPositionalValues(notification.getProfileId(), notification.getCreatedAt(),
                            notification.getType().name(), notification.getReadAt(), notification.getRelatedId(),
                            notification.getContent(), notification.getMetadata())
                    .build();

            var session = connector.getSession();
            ResultSet executed = session.execute(statement);

            return ReadNotificationMapper.fromAstraRow(executed.one());
        }, "save", notification);
    }

    @Override
    public List<ReadNotification> saveAll(List<ReadNotification> notifications, int ttl) {
        validateTtl(ttl);
        return execute(() -> {
            var list = notifications.stream()
                    .map(n -> SimpleStatement.builder(INSERT + USING_TTL.formatted(ttl))
                            .addPositionalValues(n.getProfileId(), n.getCreatedAt(),
                                    n.getType().name(), n.getReadAt(), n.getRelatedId(),
                                    n.getContent(), n.getMetadata())
                            .build())
                    .toList();

            var statement = BatchStatement.builder(BatchType.LOGGED)
                    .addStatements(new ArrayList<>(list))
                    .build();

            var session = connector.getSession();
            session.execute(statement);

            log.info("Saved {} notifications, usingTtl={}", notifications.size(), ttl);

            return null;
        }, "saveAll", notifications.size() + " notifications");
    }

    @Override
    public Optional<ReadNotification> findBy(String profileId, Instant createdAt, NotificationType type) {
        return Optional.empty();
    }

    @Override
    public ResultsPage<ReadNotification> findAllBy(String profileId, String pageState, int pageSize) {
        return null;
    }

    private void validateTtl(int ttl) {
        if (ttl <= 0) {
            throw new AstraException("TTL must be greater than 0");
        }
    }
}

package com.sojka.pomeranian.notification.repository;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.sojka.pomeranian.astra.connection.Connector;
import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.astra.exception.AstraException;
import com.sojka.pomeranian.astra.repository.AstraPageableRepository;
import com.sojka.pomeranian.notification.model.ReadNotification;
import com.sojka.pomeranian.notification.util.ReadNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.sojka.pomeranian.chat.util.Constants.NOTIFICATIONS_KEYSPACE;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReadNotificationRepositoryImpl extends AstraPageableRepository implements ReadNotificationRepository {

    private static final String READ_NOTIFICATIONS_TABLE = "read_notifications";

    private static final String INSERT = """
            INSERT INTO %s.%s ( \
            profile_id, created_at, type, read_at, related_id, related_type, content, metadata \
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)""".formatted(NOTIFICATIONS_KEYSPACE, READ_NOTIFICATIONS_TABLE);
    private static final String USING_TTL = " USING TTL %s";
    private static final String SELECT_ALL = QueryConstants.SELECT_ALL_BY_PROFILE_ID
            .formatted(NOTIFICATIONS_KEYSPACE, READ_NOTIFICATIONS_TABLE);
    private static final String COUNT_BY_PROFILE_ID = """
            SELECT COUNT(*) FROM %s.%s WHERE profile_id = ?""".formatted(NOTIFICATIONS_KEYSPACE, READ_NOTIFICATIONS_TABLE);
    private static final String DELETE_BY_PROFILE_ID = """
            DELETE FROM %s.%s WHERE profile_id = ?""".formatted(NOTIFICATIONS_KEYSPACE, READ_NOTIFICATIONS_TABLE);

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
                            notification.getRelatedType(), notification.getContent(), notification.getMetadata())
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
                                    n.getRelatedType(), n.getContent(), n.getMetadata())
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
    public ResultsPage<ReadNotification> findAllBy(String profileId, String pageState, int pageSize) {
        return execute(() -> {
            ByteBuffer pagingStateBuffer = decodePageState(pageState);

            var statement = SimpleStatement.builder(SELECT_ALL)
                    .addPositionalValues(profileId)
                    .setPageSize(pageSize)
                    .setPagingState(pagingStateBuffer)
                    .build();

            var session = connector.getSession();
            ResultSet resultSet = session.execute(statement);

            return resultsPage(resultSet, pageSize, ReadNotificationMapper::fromAstraRow);
        }, "findAllBy", profileId);
    }

    private void validateTtl(int ttl) {
        if (ttl <= 0) {
            throw new AstraException("TTL must be greater than 0");
        }
    }

    @Override
    public Optional<Long> countByIdProfileId(String profileId) {
        return execute(() -> {
            var statement = SimpleStatement.builder(COUNT_BY_PROFILE_ID).addPositionalValues(profileId).build();

            var session = connector.getSession();
            Row row = session.execute(statement).one();

            return Optional.ofNullable(row).map(r -> r.getLong(0));
        }, "countByIdProfileId", profileId);
    }

    @Override
    public void deleteAllByIdProfileId(String profileId) {
        log.trace("deleteAllByIdProfileId input: profileId={}", profileId);
        execute(() -> {
            var statement = SimpleStatement.builder(DELETE_BY_PROFILE_ID).addPositionalValues(profileId).build();

            var session = connector.getSession();
            session.execute(statement);

            return true;
        }, "deleteAllByIdProfileId", profileId);
    }
}

package com.sojka.pomeranian.notification.repository;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.sojka.pomeranian.astra.connection.Connector;
import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.astra.repository.AstraRepository;
import com.sojka.pomeranian.chat.dto.MessageNotificationDto;
import com.sojka.pomeranian.notification.model.Notification;
import com.sojka.pomeranian.notification.model.NotificationType;
import com.sojka.pomeranian.notification.util.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.sojka.pomeranian.chat.util.Constants.NOTIFICATIONS_KEYSPACE;

@Slf4j
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl extends AstraRepository<Notification> implements NotificationRepository {

    private static final String NOTIFICATIONS_TABLE = "notifications";
    private static final String INSERT = """
            INSERT INTO %s.%s ( \
            profile_id, created_at, type, related_id, content, metadata \
            ) VALUES (?, ?, ?, ?, ?, ?)""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);
    private static final String USING_TTL = " USING TTL %s";
    private static final String SELECT_BY_PRIMARY_KEY = """
            SELECT * FROM %s.%s \
            WHERE profile_id = ? \
            AND created_at = ? \
            AND type = ?""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);
    private static final String SELECT_BY_PROFILE_ID = """
            SELECT * FROM %s.%s \
            WHERE profile_id = ?""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);
    private static final String DELETE_BY = """
            DELETE FROM %s.%s \
            WHERE profile_id = ? \
            AND created_at = ? \
            AND type = ?""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);


    private final Connector connector;

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public Notification save(Notification notification, int ttl) {
        return execute(() -> {
            String dml = INSERT + (ttl > 0 ? USING_TTL.formatted(ttl) : "");
            var statement = SimpleStatement.builder(dml)
                    .addPositionalValues(notification.getProfileId(), notification.getCreatedAt(),
                            notification.getType().name(), notification.getRelatedId(),
                            notification.getContent(), notification.getMetadata())
                    .build();

            var session = connector.getSession();
            session.execute(statement);

            return notification;
        }, "save", notification);
    }

    @Override
    public Notification save(Notification notification) {
        return save(notification, -1);
    }

//    @Override
//    public List<Notification> saveAll(List<Notification> notifications, int ttl) {
//        return execute(() -> {
//            var list = notifications.stream()
//                    .map(n -> QueryBuilder.insertInto(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE)
//                            .value("profile_id", literal(n.getProfileId()))
//                            .value("created_at", literal(n.getCreatedAt()))
//                            .value("type", literal(n.getType().name()))
//                            .value("read_at", literal(n.getReadAt()))
//                            .usingTtl(ttl)
//                            .build())
//                    .toList();
//
//            var statement = BatchStatement.builder(BatchType.LOGGED)
//                    .addStatements(new ArrayList<>(list))
//                    .build();
//
//            var session = connector.getSession();
//            session.execute(statement);
//
//            log.info("Saved {} notifications, usingTtl={}", notifications.size(), ttl);
//
//            return notifications;
//        }, "saveAll", notifications.size() + " notifications");
//    }

    @Override
    public Optional<Notification> findBy(String profileId, Instant createdAt, NotificationType type) {
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

    @Override
    public void deleteAll(List<MessageNotificationDto> notifications) {
        execute(() -> {
            List<SimpleStatement> deleteStatements = notifications.stream()
                    .map(n -> SimpleStatement.builder(DELETE_BY)
                            .addPositionalValues(n.getProfileId(), n.getCreatedAt(), n.getType())
                            .build())
                    .toList();

            var statement = BatchStatement.builder(BatchType.LOGGED)
                    .addStatements(new ArrayList<>(deleteStatements))
                    .build();

            var session = connector.getSession();
            session.execute(statement);

            return "done";
        }, "findAllBy", notifications);
    }
}

package com.sojka.pomeranian.notification.repository;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.sojka.pomeranian.astra.connection.Connector;
import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.astra.repository.AstraPageableRepository;
import com.sojka.pomeranian.chat.config.ChatConfig;
import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.notification.model.NotificationModel;
import com.sojka.pomeranian.notification.util.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.sojka.pomeranian.chat.util.Constants.NOTIFICATIONS_KEYSPACE;
import static com.sojka.pomeranian.lib.util.CommonUtils.getNameOrNull;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.toInstant;

@Slf4j
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl extends AstraPageableRepository implements NotificationRepository<NotificationModel> {

    private static final String NOTIFICATIONS_TABLE = "notifications";
    private static final String INSERT = """
            INSERT INTO %s.%s (
               profile_id, created_at, type, body,
               sender_id, sender_username, sender_image_192, sender_gender, sender_role
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            USING TTL %s""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE, "%s");
    private static final String SELECT_BY_PRIMARY_KEY = """
            SELECT * FROM %s.%s \
            WHERE profile_id = ? \
            AND created_at = ? \
            AND type = ?""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);
    private static final String SELECT_BY_PROFILE_ID = QueryConstants.SELECT_ALL_BY_PROFILE_ID
            .formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);
    private static final String DELETE_BY = """
            DELETE FROM %s.%s \
            WHERE profile_id = ? \
            AND created_at = ? \
            AND type = ?""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);
    private static final String COUNT_BY_PROFILE_ID = """
            SELECT COUNT(*) FROM %s.%s WHERE profile_id = ?""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);
    private static final String DELETE_BY_PROFILE_ID = """
            DELETE FROM %s.%s WHERE profile_id = ?""".formatted(NOTIFICATIONS_KEYSPACE, NOTIFICATIONS_TABLE);

    private final Connector connector;
    private final ChatConfig config;

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public NotificationModel save(NotificationModel notification) {
        var ttl = config.getNotification().getUnread().getTtl();
        return handle(() -> {
            String dml = INSERT.formatted(ttl);
            var statement = SimpleStatement.builder(dml)
                    .addPositionalValues(
                            notification.getProfileId(), notification.getCreatedAt(),
                            notification.getType().name(), notification.getBody(),
                            notification.getSenderId(), notification.getSenderUsername(),
                            notification.getSenderImage192(), notification.getSenderGender(),
                            getNameOrNull(notification.getSenderRole()))
                    .build();

            var session = connector.getSession();
            session.execute(statement);

            return notification;
        }, "save", notification);
    }

    @Override
    public List<NotificationModel> saveAll(List<NotificationModel> notifications) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public ResultsPage<NotificationModel> findAllBy(UUID profileId, String pageState, int pageSize) {
        return handle(() -> {
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
    public void deleteAll(List<Notification<Object>> notifications) {
        handle(() -> {
            List<SimpleStatement> deleteStatements = notifications.stream()
                    .map(n -> SimpleStatement.builder(DELETE_BY)
                            .addPositionalValues(n.getProfileId(), toInstant(n.getCreatedAt()), getNameOrNull(n.getType()))
                            .build())
                    .toList();

            var statement = BatchStatement.builder(BatchType.LOGGED)
                    .addStatements(new ArrayList<>(deleteStatements))
                    .build();

            var session = connector.getSession();
            session.execute(statement);

            return true;
        }, "deleteAll", notifications);
    }

    @Override
    public Optional<Long> countByIdProfileId(UUID profileId) {
        return handle(() -> {
            var statement = SimpleStatement.builder(COUNT_BY_PROFILE_ID).addPositionalValues(profileId).build();

            var session = connector.getSession();
            Row row = session.execute(statement).one();

            return Optional.ofNullable(row).map(r -> r.getLong(0));
        }, "countByIdProfileId", profileId);
    }

    @Override
    public void deleteAllByIdProfileId(UUID profileId) {
        log.trace("deleteAllByIdProfileId input: profileId={}", profileId);
        handle(() -> {
            var statement = SimpleStatement.builder(DELETE_BY_PROFILE_ID).addPositionalValues(profileId).build();

            var session = connector.getSession();
            session.execute(statement);

            return true;
        }, "deleteAllByIdProfileId", profileId);
    }
}

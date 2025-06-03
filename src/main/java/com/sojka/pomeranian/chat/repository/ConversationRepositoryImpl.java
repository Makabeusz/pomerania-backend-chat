package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.sojka.pomeranian.chat.db.AstraConnector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Slf4j
@Repository
@RequiredArgsConstructor
@Deprecated
public class ConversationRepositoryImpl implements ConversationRepository {

    private static final String CONVERSATIONS_TABLE = "conversations";
    private static final String KEYSPACE = "messages";

    private final int pageSize = 10;

    private final AstraConnector connector;

    @Override
    public List<String> findConversations(String userId) {
        try {
            CqlSession session = connector.getSession();
            // Step 1: Fetch room_ids for the user from user_conversations
            Select roomIdSelect = QueryBuilder.selectFrom(CONVERSATIONS_TABLE)
                    .column("room_id")
                    .whereColumn("user_id").isEqualTo(literal(userId));

            SimpleStatement roomIdStatement = roomIdSelect.build()
                    .setPageSize(10000);

            ResultSet roomIdResultSet = session.execute(roomIdStatement);
            List<String> roomIds = new ArrayList<>();
            for (Row row : roomIdResultSet) {
                roomIds.add(row.getString("room_id"));
            }

            if (roomIds.isEmpty()) {
                log.info(String.format("No conversations found for user_id=%s", userId));
                return List.of();
            }

            return roomIds;
        } catch (IllegalStateException e) {
            log.error(String.format("CqlSession not initialized for user_id %s: %s", userId, e.getMessage()), e);
            throw new RuntimeException("Cassandra session not initialized", e);
        } catch (Exception e) {
            log.error(String.format("Failed to find conversations for user_id %s: %s", userId, e.getMessage()), e);
            return List.of();
        }
    }
}

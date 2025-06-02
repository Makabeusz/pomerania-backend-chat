package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.sojka.pomeranian.chat.db.AstraConnector;
import com.sojka.pomeranian.chat.model.Conversation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ConversationRepositoryImpl implements ConversationRepository {

    private static final String CONVERSATIONS_TABLE = "conversations";
    private static final String KEYSPACE = "messages";

    private final int pageSize = 10;

    private final AstraConnector connector;

    @Override
    public Conversation save(Conversation conversation) {
        var statement = QueryBuilder.insertInto(KEYSPACE, CONVERSATIONS_TABLE)
                .value("room_id", literal(conversation.getRoomId()))
                .value("user_id", literal(conversation.getUserId()))
                .build();

        try {
            CqlSession session = connector.getSession();
            session.execute(statement);

            log.info(String.format("Saved conversation: room_id=%s, user_id=%s", conversation.getRoomId(), conversation.getUserId()));
            return conversation;
        } catch (IllegalStateException e) {
            log.error(String.format("CqlSession not initialized for save: %s", e.getMessage()), e);
            throw new RuntimeException("Cassandra session not initialized", e);
        } catch (Exception e) {
            log.error(String.format("Failed to save conversation: %s", e.getMessage()), e);
            throw new RuntimeException(String.format("Failed to save conversation for room_id %s", conversation.getRoomId()), e);
        }
    }
}

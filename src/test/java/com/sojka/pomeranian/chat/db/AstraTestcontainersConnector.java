package com.sojka.pomeranian.chat.db;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.sojka.pomeranian.chat.exception.AstraConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.CassandraContainer;

@Component
public class AstraTestcontainersConnector {

    Logger log = LoggerFactory.getLogger(AstraTestcontainersConnector.class);

    private final CassandraContainer<?> cassandraContainer;
    private volatile CqlSession session;
    private final Object lock = new Object();

    public AstraTestcontainersConnector(CassandraContainer<?> cassandraContainer) {
        this.cassandraContainer = cassandraContainer;
    }

    public CqlSession connect() {
        if (session != null && !session.isClosed()) {
            return session;
        }
        try {
            log.info("Attempting to connect to Testcontainers Cassandra");
            CqlSession newSession = CqlSession.builder()
                    .addContactPoint(cassandraContainer.getContactPoint())
                    .withLocalDatacenter(cassandraContainer.getLocalDatacenter())
                    .withAuthCredentials("cassandra", "cassandra") // Default credentials
                    .withKeyspace("messages")
                    .build();

            synchronized (lock) {
                this.session = newSession;
            }

            log.info("Connected to Testcontainers Cassandra, keyspace: {}", "messages");
            return session;
        } catch (DriverException e) {
            log.error("Failed to connect to Testcontainers Cassandra: {}", e.getMessage(), e);
            throw new AstraConnectionException("Testcontainers Cassandra connection failed", e);
        } catch (Exception e) {
            log.error("Unexpected error connecting to Testcontainers Cassandra: {}", e.getMessage(), e);
            throw new AstraConnectionException("Unexpected error connecting to Testcontainers Cassandra", e);
        }
    }

    public CqlSession getSession() {
        if (session == null || session.isClosed()) {
            throw new IllegalStateException("CqlSession is not initialized or closed");
        }
        return session;
    }

    public void close() {
        if (session != null && !session.isClosed()) {
            session.close();
            log.info("Testcontainers Cassandra session closed");
        }
    }
}

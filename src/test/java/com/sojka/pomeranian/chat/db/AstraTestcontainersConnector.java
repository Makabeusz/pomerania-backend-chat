package com.sojka.pomeranian.chat.db;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.sojka.pomeranian.chat.exception.AstraConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.CassandraContainer;

import java.time.Duration;

@Primary
@Component
public class AstraTestcontainersConnector extends AstraConnector {

    Logger log = LoggerFactory.getLogger(AstraTestcontainersConnector.class);

    private final CassandraContainer<?> cassandraContainer;
    private volatile CqlSession session;
    private final Object lock = new Object();

    public AstraTestcontainersConnector(CassandraContainer<?> cassandraContainer) {
        super(null);
        this.cassandraContainer = cassandraContainer;
    }

    public CqlSession connect() {
        if (session != null && !session.isClosed()) {
            return session;
        }
        try {
            DriverConfigLoader configLoader = DriverConfigLoader.programmaticBuilder()
                    .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(5))
                    .build();
            log.info("Attempting to connect to Testcontainers Cassandra, contactPoint=({}), localdatacenter=({})",
                    cassandraContainer.getContactPoint(), cassandraContainer.getLocalDatacenter());
            CqlSession newSession = CqlSession.builder()
                    .addContactPoint(cassandraContainer.getContactPoint())
                    .withLocalDatacenter("dc1")
                    .withAuthCredentials("cassandra", "cassandra") // Default credentials
                    .withKeyspace("messages")
                    .withConfigLoader(configLoader)
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

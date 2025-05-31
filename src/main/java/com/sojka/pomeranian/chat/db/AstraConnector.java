package com.sojka.pomeranian.chat.db;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.sojka.pomeranian.chat.config.AstraConfig;
import com.sojka.pomeranian.chat.exception.AstraConnectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class AstraConnector {

    private final AstraConfig config;
    private volatile CqlSession session;
    private final Object lock = new Object();

    public void initialize() {
        try {
            connect();
            log.info("Astra connection initialized successfully");
        } catch (Exception e) {
            log.error(String.format("Failed to initialize Astra connection: %s", e.getMessage()), e);
        }
    }

    public CqlSession connect() {
        if (session != null && !session.isClosed()) {
            return session;
        }

        try {
            log.info("Attempting to connect to Astra");
            CqlSession newSession = CqlSession.builder()
                    .withCloudSecureConnectBundle(Paths.get(config.getConnectionBundleFilePath()))
                    .withAuthCredentials(config.getClientId(), config.getSecret())
                    .withKeyspace(config.getKeyspace())
                    .build();

            synchronized (lock) {
                this.session = newSession;
            }
            log.info(String.format("Connected to Astra, keyspace: %s", config.getKeyspace()));
            return session;
        } catch (DriverException e) {
            log.error(String.format("Failed to connect to Astra: %s", e.getMessage()), e);
            throw new AstraConnectionException("Astra connection failed", e);
        } catch (IllegalArgumentException e) {
            log.error(String.format("Invalid configuration: %s", e.getMessage()), e);
            throw new AstraConnectionException("Invalid configuration: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error(String.format("Unexpected error connecting to Astra: %s", e.getMessage()), e);
            throw new AstraConnectionException("Unexpected error connecting to Astra", e);
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
            log.info("Astra session closed");
        }
    }
}
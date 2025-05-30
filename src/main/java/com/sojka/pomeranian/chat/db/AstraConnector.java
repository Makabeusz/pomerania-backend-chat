package com.sojka.pomeranian.chat.db;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.sojka.pomeranian.chat.config.AstraConfig;
import com.sojka.pomeranian.chat.exception.AstraConnectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class AstraConnector {

    private final AstraConfig config;
    private volatile CqlSession session;
    private final Object lock = new Object();

    public void initialize() {
        connect().subscribe(
                session -> log.info("Astra connection initialized successfully"),
                error -> log.error("Failed to initialize Astra connection: {}", error.getMessage())
        );
    }

    public Mono<CqlSession> connect() {
        if (session != null && !session.isClosed()) {
            return Mono.just(session);
        }

        return Mono.fromCompletionStage(() -> CqlSession.builder()
                        .withCloudSecureConnectBundle(Paths.get(config.getConnectionBundleFilePath()))
                        .withAuthCredentials(config.getClientId(), config.getSecret())
                        .withKeyspace(config.getKeyspace())
                        .buildAsync())
                .doOnSubscribe(s -> log.info("Attempting to connect to Astra"))
                .doOnSuccess(s -> {
                    synchronized (lock) {
                        this.session = s;
                    }
                    log.info("Connected to Astra, keyspace: {}", config.getKeyspace());
                })
                .doOnError(e -> log.error("Failed to connect to Astra: {}", e.getMessage(), e))
                .onErrorMap(DriverException.class, e -> new AstraConnectionException("Astra connection failed", e))
                .onErrorMap(IllegalArgumentException.class, e -> new AstraConnectionException("Invalid configuration: " + e.getMessage(), e))
                .cache();
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


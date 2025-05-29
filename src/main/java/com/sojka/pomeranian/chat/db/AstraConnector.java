package com.sojka.pomeranian.chat.db;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.sojka.pomeranian.chat.config.AstraConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class AstraConnector {

    private final AstraConfig config;
    private CqlSession session;

    public CqlSession connect() {
        try {
            this.session = CqlSession.builder()
                    .withCloudSecureConnectBundle(Paths.get(config.getConnectionBundleFilePath()))
                    .withAuthCredentials(config.getClientId(), config.getSecret())
                    .withKeyspace(config.getKeyspace())
                    .build();

            Metadata metadata = this.session.getMetadata();
            for (Node node : metadata.getNodes().values()) {
                System.out.println(node);
            }
            return session;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

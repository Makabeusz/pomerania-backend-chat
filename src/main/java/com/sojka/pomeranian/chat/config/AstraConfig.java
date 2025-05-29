package com.sojka.pomeranian.chat.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Component
@ConfigurationProperties("astra")
public class AstraConfig {

    private String connectionBundleFilePath;
    private String clientId;
    private String secret;
    private String keyspace;

    @Override
    public String toString() {
        return "AstraConfig{connectionBundleFilePath='%s', clientId='%s', clientSecret='%s', keyspace='%s'}"
                .formatted(connectionBundleFilePath, truncate(clientId), truncate(secret), keyspace);
    }

    private String truncate(String s) {
        if (s != null) {
            return s.substring(0, 2) + "..." + s.substring(s.length() - 3, s.length() - 1);
        }
        return null;
    }
}

package com.sojka.pomeranian.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "pomeranian.chat")
public class ChatConfig {

    private Cache cache;
    private Purge purge;

    @Data
    public static class Cache {
        private int writeTimeoutMs;
        private Duration writeTimeoutDuration;

        public Cache(int writeTimeoutMs) {
            this.writeTimeoutMs = writeTimeoutMs;
            this.writeTimeoutDuration = Duration.ofMillis(writeTimeoutMs);
        }
    }

    @Data
    public static class Purge {
        private int batchSize;
    }
}

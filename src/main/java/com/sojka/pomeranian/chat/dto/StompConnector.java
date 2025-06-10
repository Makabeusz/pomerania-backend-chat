package com.sojka.pomeranian.chat.dto;

import lombok.Getter;

public record StompConnector(StompConnectorType type) {

    public enum StompConnectorType {

        CHAT("chat"), NOTIFICATIONS("notifications");

        @Getter
        private final String name;

        StompConnectorType(String name) {
            this.name = name;
        }
    }
}

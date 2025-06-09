package com.sojka.pomeranian.chat.dto;

import lombok.Getter;

public enum StompConnector {

    CHAT("chat"), NOTIFICATIONS("notifications");

    @Getter
    private final String name;

    StompConnector(String name) {
        this.name = name;
    }
}

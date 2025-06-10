package com.sojka.pomeranian.chat.dto;

import lombok.Getter;

public enum StompSubscription {

    CHAT("chat"), NOTIFICATIONS("notifications");

    @Getter
    private final String name;

    StompSubscription(String name) {
        this.name = name;
    }
}

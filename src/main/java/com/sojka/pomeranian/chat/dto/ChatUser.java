package com.sojka.pomeranian.chat.dto;

// TODO: move to shared api

import java.util.UUID;

public record ChatUser(UUID id, String username, UUID image192) {
}

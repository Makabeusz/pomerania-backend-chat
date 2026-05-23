package com.sojka.pomeranian.chat.dto;

import java.util.UUID;

public record ChatResetRead(
        UUID profileId,
        Long roomCount
) {
}

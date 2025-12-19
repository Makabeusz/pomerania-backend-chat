package com.sojka.pomeranian.chat.dto;

import java.util.UUID;

// TODO: move to lib
public record R2BucketDeleteRequest(
        UUID id,
        UUID userId
) {
}

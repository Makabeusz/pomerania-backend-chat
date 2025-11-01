package com.sojka.pomeranian.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The online-status cache model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveUser {

    private UUID userId;
    private Map<String, List<String>> subscriptions;
    private String simpSessionId;
    private Instant createdAt;

}

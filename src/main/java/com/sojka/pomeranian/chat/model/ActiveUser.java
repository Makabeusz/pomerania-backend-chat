package com.sojka.pomeranian.chat.model;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * The online-status cache model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveUser {

    private String userId;
    private Set<StompSubscription> subscriptions;
    private Instant createdAt;

}

package com.sojka.pomeranian.chat.model;

import com.sojka.pomeranian.chat.dto.StompSubscription;
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
    private List<Session> sessions;

    public boolean isOnline(StompSubscription subscription) {
        for (ActiveUser.Session session : this.getSessions()) {
            List<String> subs = session.getSubscriptions().get(subscription.type().name());
            if (subs != null && subs.contains(subscription.id())) {
                return true;
            }
        }
        return false;
    }
    public boolean isOnline(StompSubscription.Type type) {
        for (ActiveUser.Session session : this.getSessions()) {
            return session.getSubscriptions().containsKey(type.name());
        }
        return false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Session {
        private Map<String, List<String>> subscriptions;
        private String simpSessionId;
        private Instant createdAt;
    }

}

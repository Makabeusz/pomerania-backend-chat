package com.sojka.pomeranian.chat.service.cache;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.exception.CacheException;
import com.sojka.pomeranian.chat.model.ActiveUser;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.sojka.pomeranian.lib.util.DateTimeUtils.getCurrentInstant;

/**
 * An in-memory implementation of {@link SessionCache} for tracking active chat users.
 * Uses a thread-safe {@link ConcurrentHashMap.KeySetView} to store user IDs.
 * Suitable for single-instance deployments; not designed for distributed environments.
 * <p>
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "pomeranian.chat",
        name = "redis-enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class InMemoryLocalSessionCache implements SessionCache {

    private final Map<UUID, ActiveUser> users;
    private final Map<String, UUID> sessions;

    /**
     * Constructs an instance with a provided map of active users.
     *
     * @param users The map to store active user IDs.
     */
    public InMemoryLocalSessionCache(Map<UUID, ActiveUser> users, Map<String, UUID> sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    @Autowired
    public InMemoryLocalSessionCache() {
        this.users = new ConcurrentHashMap<>();
        this.sessions = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isOnline(UUID userId, StompSubscription subscription) {
        ActiveUser activeUser = users.get(userId);
        if (activeUser != null) {
            return activeUser.isOnline(subscription);
        }
        return false;
    }

    @Override
    public boolean isOnline(UUID userId, StompSubscription.Type type) {
        ActiveUser activeUser = users.get(userId);
        if (activeUser != null) {
            return activeUser.isOnline(type);
        }
        return false;
    }

    @Override
    public Optional<ActiveUser> get(UUID userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public List<ActiveUser> getAll() {
        return new ArrayList<>(users.values());
    }

    /**
     * Adds a user to the active users map, marking them as online.
     *
     * @param userId       The ID of the user to add.
     * @param subscription The online subscription
     * @return {@code true} if the subscription was added (was not already online)
     */
    @Override
    public boolean add(UUID userId, String simpSessionId, StompSubscription subscription) {
        ActiveUser activeUser = users.get(userId);
        if (activeUser == null) {
            throw new CacheException("User=%s is not online".formatted(userId));
        }

        for (ActiveUser.Session session : activeUser.getSessions()) {
            Map<String, List<String>> subscriptions = session.getSubscriptions();

            if (session.getSimpSessionId().equals(simpSessionId)) {
                if (subscriptions.containsKey(subscription.type().name())) {
                    List<String> ids = subscriptions.get(subscription.type().name());
                    if (ids.contains(subscription.id())) {
                        log.error("Subscription already exists: user_id={}, subscription={}", userId, subscription);
                        return false;
                    }
                    subscriptions.get(subscription.type().name()).add(subscription.id());
                } else {
                    List<String> ids = new ArrayList<>();
                    ids.add(subscription.id());
                    subscriptions.put(subscription.type().name(), ids);
                }
                return true;
            }
        }
        throw new CacheException("User=%s do not have active simpSessionID=%s".formatted(userId, simpSessionId));
    }

    @Override
    public boolean create(UUID userId, String simpSessionId) {
        ActiveUser activeUser = users.get(userId);
        var newSession = new ActiveUser.Session(new HashMap<>(), simpSessionId, getCurrentInstant());
        if (activeUser == null) {
            users.put(userId, new ActiveUser(userId, new ArrayList<>(List.of(newSession))));
        } else {
            for (ActiveUser.Session session : activeUser.getSessions()) {
                if (session.getSimpSessionId().equals(simpSessionId)) {
                    throw new CacheException("User=%s, simpSessionId=%s already online".formatted(userId, simpSessionId));
                }
            }
            activeUser.getSessions().add(newSession);
            users.put(userId, activeUser);
        }
        sessions.put(simpSessionId, userId);
        return true;
    }

    @Override
    public UUID remove(String simpSessionId) {
        UUID userId = sessions.get(simpSessionId);
        if (userId == null) {
            throw new CacheException("Session=%s not online".formatted(simpSessionId));
        }
        ActiveUser activeUser = users.get(userId);
        if (activeUser == null) {
            throw new CacheException("User=%s not online".formatted(userId));
        } else {
            activeUser.getSessions().stream()
                    .map(ActiveUser.Session::getSimpSessionId)
                    .forEach(sessions::remove);
            users.remove(userId);
            return userId;
        }
    }

    @Override
    public boolean remove(UUID userId, String simpSessionId, @NonNull List<StompSubscription> subscriptions) {
        ActiveUser activeUser = users.get(userId);
        if (activeUser != null) {
            for (StompSubscription subscription : subscriptions) {
                for (ActiveUser.Session session : activeUser.getSessions()) {
                    if (session.getSimpSessionId().equals(simpSessionId)) {
                        if (subscription.id() == null || subscription.id().isBlank()) {
                            session.getSubscriptions().remove(subscription.type().name());
                        } else {
                            var ids = session.getSubscriptions().get(subscription.type().name());
                            if (ids == null) {
                                break;
                            }
                            ids = ids.stream()
                                    .filter(id -> !id.equals(subscription.id()))
                                    .collect(Collectors.toCollection(ArrayList::new));
                            session.getSubscriptions().put(subscription.type().name(), ids);

                            if (ids.isEmpty()) {
                                session.getSubscriptions().remove(subscription.type().name());
                            }
                        }
                        break;
                    }
                }
            }
            users.put(userId, activeUser);
            return true;
        }
        throw new CacheException("User=%s is not online".formatted(userId));
    }

    /**
     * Clears all users from the active users set, effectively marking all users as offline.
     */
    @Override
    public void purge() {
        users.clear();
        sessions.clear();
    }
}

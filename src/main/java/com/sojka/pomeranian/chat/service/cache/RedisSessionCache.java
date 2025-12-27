package com.sojka.pomeranian.chat.service.cache;

import com.sojka.pomeranian.chat.config.ChatConfig;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.exception.CacheException;
import com.sojka.pomeranian.chat.model.ActiveUser;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.sojka.pomeranian.chat.config.cache.RedisConfig.ACTIVE_USER_PREFIX;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.getCurrentInstant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "pomeranian.chat",
        name = "redis-enabled",
        havingValue = "true"
)
public class RedisSessionCache implements SessionCache {

    private final RedisTemplate<UUID, ActiveUser> users;
    private final RedisTemplate<String, UUID> sessions;
    private final ChatConfig config;

    @Override
    public boolean isOnline(UUID userId, StompSubscription subscription) {
        ActiveUser activeUser = users.opsForValue().get(userId);
        if (activeUser != null) {
            return activeUser.isOnline(subscription);
        }
        return false;
    }

    @Override
    public boolean isOnline(UUID userId, StompSubscription.Type type) {
        ActiveUser activeUser = users.opsForValue().get(userId);
        if (activeUser != null) {
            return activeUser.isOnline(type);
        }
        return false;
    }

    @Override
    public Optional<ActiveUser> get(UUID userId) {
        return Optional.ofNullable(users.opsForValue().get(userId));
    }

    @Override
    public List<ActiveUser> getAll() {
        Set<UUID> allKeys = getAllKeys(users, ACTIVE_USER_PREFIX + "*");
        return users.opsForValue().multiGet(allKeys);
    }

    @Override
    public boolean add(UUID userId, String simpSessionId, StompSubscription subscription) {
        ActiveUser activeUser = users.opsForValue().get(userId);
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
                return Boolean.TRUE.equals(users.opsForValue().setIfPresent(userId, activeUser));
            }
        }
        throw new CacheException("User=%s do not have active simpSessionID=%s".formatted(userId, simpSessionId));
    }

    @Override
    public boolean create(UUID userId, String simpSessionId) {
        ActiveUser activeUser = users.opsForValue().get(userId);
        var newSession = new ActiveUser.Session(new HashMap<>(), simpSessionId, getCurrentInstant());
        if (activeUser == null) {
            users.opsForValue().set(userId, new ActiveUser(userId, new ArrayList<>(List.of(newSession))));
        } else {
            for (ActiveUser.Session session : activeUser.getSessions()) {
                if (session.getSimpSessionId().equals(simpSessionId)) {
                    throw new CacheException("User=%s, simpSessionId=%s already online".formatted(userId, simpSessionId));
                }
            }
            activeUser.getSessions().add(newSession);
            users.opsForValue().set(userId, activeUser);
        }
        sessions.opsForValue().set(simpSessionId, userId);
        return true;
    }

    @Override
    public UUID remove(String simpSessionId) {
        UUID userId = sessions.opsForValue().get(simpSessionId);
        if (userId == null) {
            throw new CacheException("Session=%s not online".formatted(simpSessionId));
        }
        ActiveUser activeUser = users.opsForValue().get(userId);
        if (activeUser == null) {
            throw new CacheException("User=%s not online".formatted(userId));
        } else {
            List<String> allSessionIds = activeUser.getSessions().stream().map(ActiveUser.Session::getSimpSessionId).toList();
            sessions.delete(allSessionIds);
            users.delete(userId);
            return userId;
        }
    }

    @Override
    public boolean remove(UUID userId, String simpSessionId, @NonNull List<StompSubscription> subscriptions) {
        ActiveUser activeUser = users.opsForValue().get(userId);
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
            users.opsForValue().set(userId, activeUser);
            return true;
        }
        throw new CacheException("User=%s is not online".formatted(userId));
    }

    /**
     * Clears all users from the active users set, effectively marking all users as offline.
     */
    @Override
    public void purge() {
        users.delete(getAllKeys(users, ACTIVE_USER_PREFIX + "*"));
        sessions.delete(getAllKeys(sessions, ACTIVE_USER_PREFIX + "*"));
    }

    private <T> Set<T> getAllKeys(RedisTemplate<T, ?> cache, String pattern) {
        Set<T> allKeys = new HashSet<>();
        cache.execute((RedisCallback<Void>) connection -> {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    T key = (T) cache.getKeySerializer().deserialize(keyBytes);
                    allKeys.add(key);
                }
            }
            return null;
        });
        return allKeys;
    }
}

package com.sojka.pomeranian.chat.service.cache;

import com.sojka.pomeranian.chat.config.ChatConfig;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.exception.CacheException;
import com.sojka.pomeranian.chat.model.ActiveUser;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.sojka.pomeranian.chat.config.cache.RedisConfig.ACTIVE_USER_PREFIX;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.getCurrentInstant;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class RedisChatCache implements ChatCache {

    private final RedisTemplate<UUID, ActiveUser> cache;
    private final ChatConfig config;

    @Override
    public boolean isOnline(UUID userId, StompSubscription subscription) {
        ActiveUser activeUser = cache.opsForValue().get(userId);
        if (activeUser != null) {
            return activeUser.getSubscriptions()
                    .getOrDefault(subscription.type().name(), Collections.emptyList())
                    .contains(subscription.id());
        }
        return false;
    }

    @Override
    public boolean isOnline(UUID userId, StompSubscription.Type type) {
        ActiveUser activeUser = cache.opsForValue().get(userId);
        if (activeUser != null) {
            return activeUser.getSubscriptions().containsKey(type.name());
        }
        return false;
    }

    @Override
    public Optional<ActiveUser> get(UUID userId) {
        return Optional.ofNullable(cache.opsForValue().get(userId));
    }

    @Override
    public List<ActiveUser> getAll() {
        Set<UUID> allKeys = getAllKeys();
        return cache.opsForValue().multiGet(allKeys);
    }

    @Override
    public boolean put(UUID userId, StompSubscription subscription) {
        ActiveUser activeUser = cache.opsForValue().get(userId);
        if (activeUser == null) {
            throw new CacheException("User=%s is not online".formatted(userId));
        }
        var subscriptions = activeUser.getSubscriptions();

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
        return Boolean.TRUE.equals(cache.opsForValue().setIfPresent(userId, activeUser));
    }

    @Override
    public boolean create(UUID userId, String simpSessionId) {
        var exists = cache.opsForValue().setIfAbsent(
                userId,
                new ActiveUser(userId, new HashMap<>(), simpSessionId, getCurrentInstant()),
                config.getCache().getWriteTimeoutDuration()
        );
        if (exists == null) {
            log.warn("Unexpected cache operation in the pipeline / transaction, userId={}", userId);
            return false;
        } else if (exists) {
            return true;
        } else {
            log.warn("User already online: {}", userId);
            return false;
        }
    }

    /**
     * Removes a user from the active users map, marking them as offline.
     *
     * @param userId The ID of the user to remove.
     * @return {@code true} if the user was removed (was online),
     * {@code false} if the user was not present.
     */
    @Override
    public boolean remove(UUID userId) {
        return cache.delete(userId);
    }

    @Override
    public boolean remove(UUID userId, @NonNull List<StompSubscription> subscriptions) {
        ActiveUser activeUser = cache.opsForValue().get(userId);
        if (activeUser != null) {
            for (StompSubscription subscription : subscriptions) {
                if (subscription.id() == null || subscription.id().isBlank()) {
                    return activeUser.getSubscriptions().remove(subscription.type().name()) != null;
                } else {
                    var ids = activeUser.getSubscriptions().get(subscription.type().name());
                    if (ids == null) {
                        return false;
                    }
                    ids = ids.stream()
                            .filter(id -> !id.equals(subscription.id()))
                            .collect(Collectors.toCollection(ArrayList::new));
                    activeUser.getSubscriptions().put(subscription.type().name(), ids);

                    if (ids.isEmpty()) {
                        return activeUser.getSubscriptions().remove(subscription.type().name()) != null;
                    }
                    return true;
                }
            }
            if (activeUser.getSubscriptions().isEmpty()) {
                cache.delete(userId);
            } else {
                cache.opsForValue().set(userId, activeUser);
            }
        }
        return false;
    }

    /**
     * Clears all users from the active users set, effectively marking all users as offline.
     */
    @Override
    public void purge() {
        Set<UUID> allKeys = getAllKeys();
        cache.delete(allKeys);
    }

    private Set<UUID> getAllKeys() {
        Set<UUID> allKeys = new HashSet<>();
        cache.execute((RedisCallback<Void>) connection -> {
            ScanOptions options = ScanOptions.scanOptions().match(ACTIVE_USER_PREFIX + "*").count(100).build();
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    UUID key = (UUID) cache.getKeySerializer().deserialize(keyBytes);
                    allKeys.add(key);
                }
            }
            return null;
        });
        return allKeys;
    }
}

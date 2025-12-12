package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.model.ActiveUser;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.sojka.pomeranian.lib.util.DateTimeUtils.getCurrentInstant;

/**
 * An in-memory implementation of {@link ChatCacheInt} for tracking active chat users.
 * Uses a thread-safe {@link ConcurrentHashMap.KeySetView} to store user IDs.
 * Suitable for single-instance deployments; not designed for distributed environments.
 * <p>
 */
@Slf4j
@Component
public abstract class ChatCacheA implements ChatCacheInt {

    abstract ActiveUser getValue(UUID key);
    abstract List<ActiveUser> getAllValues();
    abstract boolean deleteEntry(UUID key);
    abstract Boolean putEntry(UUID key, ActiveUser value);
    abstract void purgeAll();

    /**
     * Checks if a user specific subscription is online.
     *
     * @param userId The ID of the user to check.
     * @return {@code true} if the user is online, {@code false} otherwise.
     */
    @Override
    public boolean isOnline(UUID userId, StompSubscription subscription) {
        ActiveUser activeUser = getValue(userId);
        if (activeUser != null) {
            return activeUser.getSubscriptions()
                    .getOrDefault(subscription.type().name(), Collections.emptyList())
                    .contains(subscription.id());
        }
        return false;
    }

    /**
     * Checks if a user is online.
     *
     * @param userId The ID of the user to check.
     * @return {@code true} if the user is online, {@code false} otherwise.
     */
    @Override
    public boolean isOnline(UUID userId, StompSubscription.Type type) {
        ActiveUser activeUser = getValue(userId);
        if (activeUser != null) {
            return activeUser.getSubscriptions().containsKey(type.name());
        }
        return false;
    }

    @Override
    public Optional<ActiveUser> get(UUID userId) {
        return Optional.ofNullable(getValue(userId));
    }

    @Override
    public List<ActiveUser> getAll() {
        return getAllValues();
    }

    /**
     * Adds a user to the active users map, marking them as online.
     *
     * @param userId       The ID of the user to add.
     * @param subscription The online subscription
     * @return {@code true} if the subscription was added (was not already online)
     */
    @Override
    public boolean put(UUID userId, StompSubscription subscription) {
        ActiveUser activeUser = getValue(userId);
        if (activeUser == null) {
            log.error("The user session does not exists");
            return false;
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

        return true;
    }

    @Override
    public boolean create(UUID userId, String simpSessionId) {
        var exists =  putEntry(userId, new ActiveUser(userId, new HashMap<>(), simpSessionId, getCurrentInstant()));
        if (exists == null) {
            log.warn("Unexpected cache operation in the pipeline / transaction, userId={}", userId);
            return false;
        } else if (exists) {
            log.info("TODO: remove me, cache entry created, all good, user={} ", userId);
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
        return deleteEntry(userId);
    }

    @Override
    public boolean remove(UUID userId, @NonNull List<StompSubscription> subscriptions) {
        ActiveUser activeUser = getValue(userId);
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
                deleteEntry(userId);
            }
        }
        return false;
    }

    /**
     * Clears all users from the active users set, effectively marking all users as offline.
     */
    @Override
    public void purge() {
        purgeAll();
    }
}

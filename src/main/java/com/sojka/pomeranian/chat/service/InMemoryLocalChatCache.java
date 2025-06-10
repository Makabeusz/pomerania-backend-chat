package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.model.ActiveUser;
import com.sojka.pomeranian.chat.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory implementation of {@link ChatCache} for tracking active chat users.
 * Uses a thread-safe {@link ConcurrentHashMap.KeySetView} to store user IDs.
 * Suitable for single-instance deployments; not designed for distributed environments.
 * <p>
 * todo: make it a proper cache with normal entries and TTL - if not refreshed for say 30 minutes
 *       assume the user is offline and remove cache entry
 */
@Component
public class InMemoryLocalChatCache implements ChatCache {

    private final Map<String, ActiveUser> activeUsers;

    /**
     * Constructs an instance with a provided map of active users.
     *
     * @param activeUsers The map to store active user IDs.
     */
    public InMemoryLocalChatCache(Map<String, ActiveUser> activeUsers) {
        this.activeUsers = activeUsers;
    }

    /**
     * Default constructor that initializes an empty thread-safe map for active users.
     */
    @Autowired
    public InMemoryLocalChatCache() {
        this.activeUsers = new ConcurrentHashMap<>();
    }

    /**
     * Checks if a user is online (present in the active users set).
     *
     * @param userId The ID of the user to check.
     * @return {@code true} if the user is online, {@code false} otherwise.
     */
    @Override
    public boolean isOnline(String userId, StompSubscription subscription) {
        ActiveUser activeUser = activeUsers.get(userId);
        if (activeUser != null) {
            return activeUser.getSubscriptions().contains(subscription);
        }
        return false;
    }

    @Override
    public Optional<ActiveUser> get(String userId) {
        throw new RuntimeException("not implemented");
    }

    /**
     * Adds a user to the active users map, marking them as online.
     *
     * @param userId       The ID of the user to add.
     * @param subscription The online subscription
     * @return {@code true} if the user was added (was not already online),
     * {@code false} if the user was already present.
     */
    @Override
    public boolean put(String userId, StompSubscription subscription) {
        ActiveUser activeUser = activeUsers.get(userId);
        if (activeUser == null) {
            activeUsers.put(userId, new ActiveUser(userId, Set.of(subscription), CommonUtils.getCurrentInstant()));
            return true;
        } else {
            var subscriptions = new HashSet<>(activeUser.getSubscriptions());
            boolean added = subscriptions.add(subscription);
            if (added) {
                activeUser.setSubscriptions(subscriptions);
                activeUsers.put(userId, activeUser);
                return true;
            }
        }

        return false;
    }

    /**
     * Removes a user from the active users map, marking them as offline.
     *
     * @param userId The ID of the user to remove.
     * @return {@code true} if the user was removed (was online),
     * {@code false} if the user was not present.
     */
    @Override
    public boolean remove(String userId) {
        return activeUsers.remove(userId) != null;
    }

    /**
     * Clears all users from the active users set, effectively marking all users as offline.
     */
    @Override
    public void purge() {
        activeUsers.clear();
    }
}
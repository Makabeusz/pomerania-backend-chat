package com.sojka.pomeranian.chat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-memory implementation of {@link ChatCache} for tracking active chat users.
 * Uses a thread-safe {@link ConcurrentHashMap.KeySetView} to store user IDs.
 * Suitable for single-instance deployments; not designed for distributed environments.
 */
@Component
public class InMemoryLocalChatCache implements ChatCache {

    private final Set<String> activeUsers;

    /**
     * Constructs an instance with a provided set of active users.
     *
     * @param activeUsers The set to store active user IDs.
     */
    public InMemoryLocalChatCache(Set<String> activeUsers) {
        this.activeUsers = activeUsers;
    }

    /**
     * Default constructor that initializes an empty thread-safe set for active users.
     */
    @Autowired
    public InMemoryLocalChatCache() {
        this.activeUsers = ConcurrentHashMap.newKeySet();
    }

    /**
     * Checks if a user is online (present in the active users set).
     *
     * @param userId The ID of the user to check.
     * @return {@code true} if the user is online, {@code false} otherwise.
     */
    @Override
    public boolean isOnline(String userId) {
        return activeUsers.contains(userId);
    }

    /**
     * Adds a user to the active users set, marking them as online.
     *
     * @param userId The ID of the user to add.
     * @return {@code true} if the user was added (was not already online),
     *         {@code false} if the user was already present.
     */
    @Override
    public boolean put(String userId) {
        return activeUsers.add(userId);
    }

    /**
     * Removes a user from the active users set, marking them as offline.
     *
     * @param userId The ID of the user to remove.
     * @return {@code true} if the user was removed (was online),
     *         {@code false} if the user was not present.
     */
    @Override
    public boolean remove(String userId) {
        return activeUsers.remove(userId);
    }

    /**
     * Clears all users from the active users set, effectively marking all users as offline.
     */
    @Override
    public void purge() {
        activeUsers.clear();
    }
}
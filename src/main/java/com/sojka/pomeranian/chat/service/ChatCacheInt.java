package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.model.ActiveUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * TODO: Add docs here, implementation don't need too much docs.
 */
public interface ChatCacheInt {

    /**
     * Checks if a user specific subscription is active.
     *
     * @param userId The ID of the user to check.
     * @return {@code true} if the user is online, {@code false} otherwise.
     */
    boolean isOnline(UUID userId, StompSubscription subscription);

    /**
     * Checks if a user is online.
     *
     * @param userId The ID of the user to check.
     * @return {@code true} if the user is online, {@code false} otherwise.
     */
    boolean isOnline(UUID userId, StompSubscription.Type type);

    /**
     * Get cache entry
     *
     * @param userId The ID of the user to check.
     * @return Cached {@link ActiveUser} data or null if user is offline
     */
    Optional<ActiveUser> get(UUID userId);

    List<ActiveUser> getAll();

    /**
     * Adds another subscription for the online user.
     *
     * @param userId       The ID of the user to add.
     * @param subscription The subscription to add
     * @return {@code true} if the subscription was added, {@code false} if already online
     * @throws RuntimeException If user is not online
     */
    boolean put(UUID userId, StompSubscription subscription);

    /**
     * Adds a user to the active users cache, marking them as online.
     *
     * @param userId        The ID of the user to add.
     * @param simpSessionId The session ID
     * @return {@code true} if the subscription was added (was not already online)
     */
    boolean create(UUID userId, String simpSessionId);

    boolean remove(UUID userId);

    boolean remove(UUID userId, List<StompSubscription> subscription);

    void purge();
}

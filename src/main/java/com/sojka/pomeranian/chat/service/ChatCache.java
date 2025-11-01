package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.model.ActiveUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * TODO: Add docs here, implementation don't need too much docs.
 */
public interface ChatCache {

    boolean isOnline(UUID userId, StompSubscription subscription);

    boolean isOnline(UUID userId, StompSubscription.Type type);

    /**
     * Get cache entry
     *
     * @param userId The ID of the user to check.
     * @return Cached {@link ActiveUser} data or null if user is offline
     */
    Optional<ActiveUser> get(UUID userId);

    List<ActiveUser> getAll();

    boolean put(UUID userId, StompSubscription subscription);

    boolean create(UUID userId, String simpSessionId);

    boolean remove(UUID userId);

    boolean remove(UUID userId, List<StompSubscription> subscription);

    void purge();
}

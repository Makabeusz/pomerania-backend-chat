package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.model.ActiveUser;

import java.util.List;
import java.util.Optional;


/**
 * TODO: Add docs here, implementation don't need too much docs.
 */
public interface ChatCache {

    boolean isOnline(String userId, StompSubscription subscription);
    boolean isOnline(String userId, StompSubscription.Type type);

    /**
     * Get cache entry
     *
     * @param userId The ID of the user to check.
     * @return Cached {@link ActiveUser} data or null if user is offline
     */
    Optional<ActiveUser> get(String userId);

    List<ActiveUser> getAll();

    boolean put(String userId, StompSubscription subscription);

    boolean create(String userId, String simpSessionId);

    boolean remove(String userId);

    boolean remove(String userId, List<StompSubscription> subscription);

    void purge();
}

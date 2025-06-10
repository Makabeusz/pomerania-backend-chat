package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.model.ActiveUser;

import java.util.Optional;

public interface ChatCache {

    boolean isOnline(String userId, StompSubscription subscription);

    /**
     * Get cache entry
     *
     * @param userId The ID of the user to check.
     * @return Cached {@link ActiveUser} data or null if user is offline
     */
    Optional<ActiveUser> get(String userId);

    boolean put(String userId, StompSubscription subscription);

    boolean remove(String userId);

    void purge();
}

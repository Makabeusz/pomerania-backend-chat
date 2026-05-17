package com.sojka.pomeranian.notification.repository;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.astra.exception.AstraException;
import com.sojka.pomeranian.lib.dto.NotificationPrimaryKey;
import com.sojka.pomeranian.lib.dto.NotificationType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository<N> {

    /**
     * Saves a notification to the database.
     *
     * @param notification The {@link N} notification to be saved.
     * @return The saved {@link N} object, unchanged from the input.
     * @throws AstraException If save fails.
     */
    N save(N notification);

    List<N> saveAll(List<N> notifications);

    Optional<N> find(UUID profileId, Instant createdAt, NotificationType type);

    Optional<N> find(NotificationPrimaryKey key);

    ResultsPage<N> findAllBy(UUID profileId, String pageState, int pageSize);

    void deleteAll(List<? extends NotificationPrimaryKey> notifications);

    Optional<Long> countByIdProfileId(UUID profileId);

    void deleteAllByIdProfileId(UUID profileId);

    void delete(UUID profileId, Instant createdAt, NotificationType type);

    void delete(NotificationPrimaryKey key);
}

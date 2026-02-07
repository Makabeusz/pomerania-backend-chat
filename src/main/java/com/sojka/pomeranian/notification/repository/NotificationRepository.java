package com.sojka.pomeranian.notification.repository;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.astra.exception.AstraException;
import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.lib.dto.NotificationType;
import com.sojka.pomeranian.notification.model.NotificationModel;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    /**
     * Saves a notification to the database with an optional time-to-live (TTL) setting.
     *
     * @param notification The {@link NotificationModel} object to be saved.
     * @param ttl          The time-to-live value in seconds for the notification. If greater than 0, the
     *                     notification will expire after the specified duration; if 0 or negative, no TTL is applied.
     * @return The saved {@link NotificationModel} object, unchanged from the input.
     * @throws AstraException If an error occurs during the save operation, wrapping the original exception.
     */
    NotificationModel save(NotificationModel notification, int ttl);

    /**
     * Saves a notification to the database.
     *
     * @param notification The {@link NotificationModel} object to be saved.
     * @return The saved {@link NotificationModel} object, unchanged from the input.
     * @throws AstraException If an error occurs during the save operation, wrapping the original exception.
     */
    NotificationModel save(NotificationModel notification);

    Optional<NotificationModel> findById(UUID profileId, Instant createdAt, NotificationType type);

    ResultsPage<NotificationModel> findAllBy(UUID profileId, String pageState, int pageSize);

    void deleteAll(List<Notification<Map<String, Object>>> notifications);

    Optional<Long> countByIdProfileId(UUID profileId);

    void deleteAllByIdProfileId(UUID profileId);
}

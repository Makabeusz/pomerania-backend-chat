package com.sojka.pomeranian.notification.repository;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.astra.exception.AstraException;
import com.sojka.pomeranian.lib.dto.NotificationDto;
import com.sojka.pomeranian.notification.model.Notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    /**
     * Saves a notification to the database with an optional time-to-live (TTL) setting.
     *
     * @param notification The {@link Notification} object to be saved.
     * @param ttl          The time-to-live value in seconds for the notification. If greater than 0, the
     *                     notification will expire after the specified duration; if 0 or negative, no TTL is applied.
     * @return The saved {@link Notification} object, unchanged from the input.
     * @throws AstraException If an error occurs during the save operation, wrapping the original exception.
     */
    Notification save(Notification notification, int ttl);

    /**
     * Saves a notification to the database.
     *
     * @param notification The {@link Notification} object to be saved.
     * @return The saved {@link Notification} object, unchanged from the input.
     * @throws AstraException If an error occurs during the save operation, wrapping the original exception.
     */
    Notification save(Notification notification);

    Optional<Notification> findById(UUID profileId, Instant createdAt, NotificationDto.Type type);

    ResultsPage<Notification> findAllBy(UUID profileId, String pageState, int pageSize);

    void deleteAll(List<NotificationDto> notifications);

    Optional<Long> countByIdProfileId(UUID profileId);

    void deleteAllByIdProfileId(UUID profileId);
}

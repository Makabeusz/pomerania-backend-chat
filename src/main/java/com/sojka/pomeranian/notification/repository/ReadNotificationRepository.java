package com.sojka.pomeranian.notification.repository;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.astra.exception.AstraException;
import com.sojka.pomeranian.notification.model.NotificationType;
import com.sojka.pomeranian.notification.model.ReadNotification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReadNotificationRepository {

    /**
     * Saves a notification to the database with an optional time-to-live (TTL) setting.
     *
     * @param notification The {@link ReadNotification} object to be saved.
     * @param ttl          The time-to-live value in seconds for the notification. If greater than 0, the
     *                     notification will expire after the specified duration; if 0 or negative, no TTL is applied.
     * @return The saved {@link ReadNotification} object, unchanged from the input.
     * @throws AstraException If an error occurs during the save operation, wrapping the original exception.
     */
    ReadNotification save(ReadNotification notification, int ttl);

    List<ReadNotification> saveAll(List<ReadNotification> notifications, int ttl);

    Optional<ReadNotification> findBy(String profileId, Instant createdAt, NotificationType type);

    ResultsPage<ReadNotification> findAllBy(String profileId, String pageState, int pageSize);

}

package com.sojka.pomeranian.notification.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.cache.SessionCache;
import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.lib.dto.NotificationType;
import com.sojka.pomeranian.notification.model.NotificationModel;
import com.sojka.pomeranian.notification.model.ReadNotification;
import com.sojka.pomeranian.notification.repository.NotificationRepository;
import com.sojka.pomeranian.notification.util.NotificationMapper;
import com.sojka.pomeranian.notification.util.ReadNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static com.sojka.pomeranian.chat.util.Constants.NOTIFY_DESTINATION;
import static com.sojka.pomeranian.lib.util.CommonUtils.noSuchElementException;
import static com.sojka.pomeranian.lib.util.DateTimeUtils.getCurrentInstant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository<NotificationModel> notificationRepository;
    private final NotificationRepository<ReadNotification> readNotificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SessionCache cache;

    /**
     * Save and public notification.<br>
     * Unread TTL is 365 days by default = 31536000 s
     *
     * @return CreatedAt
     */
    public Instant process(Notification<Object> notification) {
        Instant createdAt = getCurrentInstant();
        if (notification.getType().isShouldPersist()) {
            NotificationModel domain = NotificationMapper.toDomain(notification);
            domain.setCreatedAt(createdAt);
            notificationRepository.save(domain);
        }
        boolean online = cache.isOnline(notification.getProfileId(), StompSubscription.Type.CHAT_NOTIFICATIONS);
        if (online) {
            messagingTemplate.convertAndSendToUser(notification.getProfileId() + "", NOTIFY_DESTINATION, notification);
        }
        log.debug("Published notification type={} to userId={}, isOnline={}", notification.getType(), notification.getProfileId(), online);

        return createdAt;
    }

    /**
     * Deletes unread and saves read notification.<br>
     * Read TTL is 30 days by default = 2592000s
     *
     * @return CreatedAt
     */
    public Instant markRead(Notification.PrimaryKey key) {
        var readAt = getCurrentInstant();

        var notification = notificationRepository.find(key).orElseThrow(noSuchElementException("unread notification", key));
        notificationRepository.delete(key);
        readNotificationRepository.save(ReadNotificationMapper.toReadNotificationDomain(notification, readAt));

        log.debug("Marked notifications as read {}", key);

        return readAt;
    }

    public ResultsPage<Notification<Object>> getUnread(UUID profileId, String pageState, int pageSize) {
        var resultsPage = notificationRepository.findAllBy(profileId, pageState, pageSize);
        var notifications = resultsPage.getResults().stream()
                .map(NotificationMapper::toDto)
                .toList();

        return new ResultsPage<>(notifications, resultsPage.getNextPageState());
    }

    public Long countUnreadNotifications(UUID userId) {
        var count = notificationRepository.countByIdProfileId(userId).orElseThrow();

        log.debug("Fetched {} unread notifications count", count);
        return count;
    }

    public ResultsPage<Notification<Object>> getRead(UUID profileId, String pageState, int pageSize) {
        var resultsPage = readNotificationRepository.findAllBy(profileId, pageState, pageSize);
        var notifications = resultsPage.getResults().stream()
                .map(ReadNotificationMapper::toDto)
                .toList();

        return new ResultsPage<>(notifications, resultsPage.getNextPageState());
    }

    @Transactional
    public long deleteUserNotifications(UUID userId) {
        var deletedUserNotifications = notificationRepository.countByIdProfileId(userId).orElseThrow();
        notificationRepository.deleteAllByIdProfileId(userId);
        log.info("Removed {} notifications of userID={}", deletedUserNotifications, userId);
        return deletedUserNotifications;
    }

    @Transactional
    public long deleteUserReadNotifications(UUID userId) {
        var deletedUserNotifications = notificationRepository.countByIdProfileId(userId).orElseThrow();
        readNotificationRepository.deleteAllByIdProfileId(userId);
        log.info("Removed {} read notifications of userID={}", deletedUserNotifications, userId);
        return deletedUserNotifications;
    }

    public void deleteRead(UUID userId, Instant createdAt, NotificationType type) {
        readNotificationRepository.delete(userId, createdAt, type);
        log.info("Removed single read notification: userId={} createdAt={}, type={}", userId, createdAt, type);
    }

    public void deleteUnread(UUID userId, Instant createdAt, NotificationType type) {
        notificationRepository.delete(userId, createdAt, type);
        log.info("Removed single unread notification: userId={} createdAt={}, type={}", userId, createdAt, type);
    }

}

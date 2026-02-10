package com.sojka.pomeranian.notification.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.cache.SessionCache;
import com.sojka.pomeranian.lib.dto.Notification;
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
import java.util.List;
import java.util.UUID;

import static com.sojka.pomeranian.chat.util.Constants.NOTIFY_DESTINATION;
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
        NotificationModel domain = NotificationMapper.toDomain(notification);
        domain.setCreatedAt(getCurrentInstant());

        notificationRepository.save(domain);
        boolean online = cache.isOnline(notification.getProfileId(), StompSubscription.Type.CHAT_NOTIFICATIONS);
        if (online) {
            messagingTemplate.convertAndSendToUser(notification.getProfileId() + "", NOTIFY_DESTINATION, notification);
        }
        log.debug("Published notification type={} to userId={}, isOnline={}",
                notification.getType(), notification.getProfileId(), online);

        return domain.getCreatedAt();
    }

    /**
     * Deletes unread and saves read notification.<br>
     * Read TTL is 30 days by default = 2592000 s
     *
     * @return CreatedAt
     */
    public Instant markRead(UUID userId, List<Notification<Object>> notifications) {
        boolean allAreUserNotifications = notifications.stream().allMatch(n -> userId.equals(n.getProfileId()));
        if (!allAreUserNotifications) {
            throw new SecurityException("User can mark as read only its own notifications. userId=%s, notifications=%s"
                    .formatted(userId, notifications));
        }
        var readAt = getCurrentInstant();

        notificationRepository.deleteAll(notifications);
        readNotificationRepository.saveAll(notifications.stream()
                .map(n -> ReadNotificationMapper.toReadNotificationDomain(n, readAt))
                .toList());

        log.info("Marked {} notifications as read", notifications);

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
        notificationRepository.deleteAllByIdProfileId(userId);
        log.info("Removed {} read notifications of userID={}", deletedUserNotifications, userId);
        return deletedUserNotifications;
    }

}

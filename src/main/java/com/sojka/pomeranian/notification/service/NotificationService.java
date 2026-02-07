package com.sojka.pomeranian.notification.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.cache.SessionCache;
import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.notification.model.NotificationModel;
import com.sojka.pomeranian.notification.repository.NotificationRepository;
import com.sojka.pomeranian.notification.repository.ReadNotificationRepository;
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

    private final NotificationRepository notificationRepository;
    private final ReadNotificationRepository readNotificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SessionCache cache;

    /**
     * @return CreatedAt
     */
    public Instant publish(Notification notification) {
        NotificationModel domain = NotificationMapper.toDomain(notification);
        domain.setCreatedAt(getCurrentInstant());

        notificationRepository.save(domain);
//        var dto = new NotificationResponse<>(notification, notification.getType().name());
        boolean online = cache.isOnline(notification.getProfileId(), StompSubscription.Type.CHAT_NOTIFICATIONS);
//        log.trace("Is user with username={} and userID={} online? {}",
//                notification.getMetadata() != null ? notification.getMetadata().get("senderId") : "null", notification.getProfileId(), online);
        if (online) {
            log.debug("Published notification type={} to userId={}, isOnline={}",
                    notification.getType(), notification.getProfileId(), online);
            messagingTemplate.convertAndSendToUser(notification.getProfileId() + "", NOTIFY_DESTINATION, notification);
        }

        return domain.getCreatedAt();
    }

    public Instant markRead(UUID userId, List<Notification> notifications) {
        boolean allAreUserNotifications = notifications.stream().allMatch(n -> userId.equals(n.getProfileId()));
        if (!allAreUserNotifications) {
            throw new SecurityException("User can mark as read only its own notifications. userId=%s, notifications=%s"
                    .formatted(userId, notifications));
        }
        var readAt = getCurrentInstant();

        notificationRepository.deleteAll(notifications);
        readNotificationRepository.saveAll(notifications.stream()
                .map(n -> ReadNotificationMapper.toReadNotificationDomain(n, readAt))
                .toList(), 3600); // 30  days TTL = 155520000 todo: parametrise that

        log.info("Marked {} notifications as read", notifications);

        return readAt;
    }

    public ResultsPage<Notification> getUnread(UUID profileId, String pageState, int pageSize) {
        var resultsPage = notificationRepository.findAllBy(profileId, pageState, pageSize);
        System.out.println(resultsPage);
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

    public ResultsPage<Notification> getRead(UUID profileId, String pageState, int pageSize) {
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

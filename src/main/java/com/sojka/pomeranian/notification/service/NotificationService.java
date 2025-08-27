package com.sojka.pomeranian.notification.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.NotificationResponse;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.ChatCache;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.notification.dto.NotificationDto;
import com.sojka.pomeranian.notification.model.Notification;
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

import static com.sojka.pomeranian.chat.util.Constants.NOTIFY_DESTINATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ReadNotificationRepository readNotificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatCache cache;

    public NotificationResponse<NotificationDto> publish(NotificationDto notification) {
        Notification domain = NotificationMapper.toDomain(notification);
        domain.setCreatedAt(CommonUtils.getCurrentInstant());

        var saved = notificationRepository.save(domain);
        var dto = new NotificationResponse<>(NotificationMapper.toDto(saved), saved.getType().name());

        boolean online = cache.isOnline(notification.getProfileId(), StompSubscription.Type.CHAT_NOTIFICATIONS);
        if (online) {
            messagingTemplate.convertAndSendToUser(notification.getProfileId(), NOTIFY_DESTINATION, dto);
        }

        log.info("Published notification: {}, isOnline={}", dto, online);

        return dto;
    }

    public Instant markRead(String userId, List<NotificationDto> notifications) {
        boolean allAreUserNotifications = notifications.stream().allMatch(n -> userId.equals(n.getProfileId()));
        if (!allAreUserNotifications) {
            throw new SecurityException("User can mark as read only its own notifications");
        }
        var readAt = CommonUtils.getCurrentInstant();

        notificationRepository.deleteAll(notifications);
        readNotificationRepository.saveAll(notifications.stream()
                .map(n -> ReadNotificationMapper.toReadNotificationDomain(n, readAt))
                .toList(), 3600); // 30  days TTL = 155520000 todo: parametrise that

        log.info("Marked {} notifications as read", notifications);

        return readAt;
    }

    public ResultsPage<NotificationDto> getUnread(String profileId, String pageState, int pageSize) {
        var resultsPage = notificationRepository.findAllBy(profileId, pageState, pageSize);
        var notifications = resultsPage.getResults().stream()
                .map(NotificationMapper::toDto)
                .toList();

        return new ResultsPage<>(notifications, resultsPage.getNextPageState());
    }

    public Long countUnreadNotifications(String userId) {
        var count = notificationRepository.countByIdProfileId(userId).orElseThrow();

        log.info("Fetched {} unread notifications count", count);
        return count;
    }

    public ResultsPage<NotificationDto> getRead(String profileId, String pageState, int pageSize) {
        var resultsPage = readNotificationRepository.findAllBy(profileId, pageState, pageSize);
        var notifications = resultsPage.getResults().stream()
                .map(ReadNotificationMapper::toDto)
                .toList();

        return new ResultsPage<>(notifications, resultsPage.getNextPageState());
    }

    @Transactional
    public long deleteUserNotifications(String userId) {
        var deletedUserNotifications = notificationRepository.countByIdProfileId(userId).orElseThrow();
        notificationRepository.deleteAllByIdProfileId(userId);
        log.info("Removed {} notifications of userID={}", deletedUserNotifications, userId);
        return deletedUserNotifications;
    }

    @Transactional
    public long deleteUserReadNotifications(String userId) {
        var deletedUserNotifications = notificationRepository.countByIdProfileId(userId).orElseThrow();
        notificationRepository.deleteAllByIdProfileId(userId);
        log.info("Removed {} notifications of userID={}", deletedUserNotifications, userId);
        return deletedUserNotifications;
    }

}

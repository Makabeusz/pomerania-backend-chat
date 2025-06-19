package com.sojka.pomeranian.notification.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.MessageNotificationDto;
import com.sojka.pomeranian.chat.dto.NotificationResponse;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.ChatCache;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.notification.dto.NotificationDto;
import com.sojka.pomeranian.notification.model.Notification;
import com.sojka.pomeranian.notification.model.NotificationType;
import com.sojka.pomeranian.notification.repository.NotificationRepository;
import com.sojka.pomeranian.notification.repository.ReadNotificationRepository;
import com.sojka.pomeranian.notification.util.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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

    public String markRead(String userId, List<MessageNotificationDto> notifications) {
        boolean allAreUserNotifications = notifications.stream().allMatch(n -> userId.equals(n.getProfileId()));
        if (!allAreUserNotifications) {
            throw new SecurityException("User can mark as read only its own notifications");
        }
        String readAt = CommonUtils.formatToDateString(CommonUtils.getCurrentInstant());

        notificationRepository.deleteAll(notifications);
        //noinspection SimplifyStreamApiCallChains - suppress 'can be replaced with peek'
        readNotificationRepository.saveAll(notifications.stream()
                        .map(n -> {
                            n.setReadAt(readAt);
                            return n;
                        })
                        .toList()
                , 155520000); // 30 days TTL

        log.info("Marked {} notifications as read", notifications);

        return readAt;
    }

    public NotificationDto get(String profileId, Instant createdAt, NotificationType type) {
        return notificationRepository.findBy(profileId, createdAt, type)
                .map(NotificationMapper::toDto)
                .orElse(null);
    }

    public ResultsPage<NotificationDto> get(String profileId, String pageState, int pageSize) {
        var resultsPage = notificationRepository.findAllBy(profileId, pageState, pageSize);
        var notifications = resultsPage.getResults().stream()
                .map(NotificationMapper::toDto)
                .toList();

        return new ResultsPage<>(notifications, resultsPage.getNextPageState());
    }

}

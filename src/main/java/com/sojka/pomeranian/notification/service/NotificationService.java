package com.sojka.pomeranian.notification.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.NotificationDto;
import com.sojka.pomeranian.chat.dto.NotificationResponse;
import com.sojka.pomeranian.chat.dto.ReadNotificationDto;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.ChatCache;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.notification.model.Notification;
import com.sojka.pomeranian.notification.repository.NotificationRepository;
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

    public Instant markRead(String userId, List<ReadNotificationDto> dto) {
        Instant now = CommonUtils.getCurrentInstant();
        List<Notification> list = dto.stream()
                .map(d -> Notification.builder()
                        .profileId(userId)
                        .createdAt(CommonUtils.formatToInstant(d.createdAt()))
                        .type(Notification.Type.valueOf(d.type()))
                        .readAt(now)
                        .build())
                .toList();
        notificationRepository.saveAll(list, 3600); // TODO: parametrise read TTL

        log.info("Marked {} notifications as read", dto);

        return now;
    }

    public NotificationDto get(String profileId, Instant createdAt, Notification.Type type) {
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

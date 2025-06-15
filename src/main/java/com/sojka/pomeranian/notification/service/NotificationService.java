package com.sojka.pomeranian.notification.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.notification.dto.NotificationDto;
import com.sojka.pomeranian.notification.model.Notification;
import com.sojka.pomeranian.notification.repository.NotificationRepository;
import com.sojka.pomeranian.notification.util.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static com.sojka.pomeranian.chat.util.Constants.NOTIFY_DESTINATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int TWO_DAYS_SECONDS = 172800;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationDto publish(NotificationDto notification) {
        var saved = notificationRepository.save(NotificationMapper.toDomain(notification));
        var dto = NotificationMapper.toDto(saved);

        messagingTemplate.convertAndSendToUser(notification.getProfileId(), NOTIFY_DESTINATION, dto);

        log.info("Published notification: {}", dto);

        return dto;
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

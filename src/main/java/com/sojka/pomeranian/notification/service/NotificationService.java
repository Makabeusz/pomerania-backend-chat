package com.sojka.pomeranian.notification.service;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.notification.dto.NotificationDto;
import com.sojka.pomeranian.notification.model.Notification;
import com.sojka.pomeranian.notification.repository.NotificationRepository;
import com.sojka.pomeranian.notification.util.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int TWO_DAYS_SECONDS = 172800;
    private final NotificationRepository notificationRepository;

    public Notification publish(Notification notification) {
        int ttl = notification.getReadAt() != null ? TWO_DAYS_SECONDS : -1;
        return notificationRepository.save(notification, ttl);
    }

    public Notification save(Notification notification) {
        int ttl = notification.getReadAt() != null ? TWO_DAYS_SECONDS : -1;
        return notificationRepository.save(notification, ttl);
    }

    public Notification save(Notification notification, int ttl) {
        return notificationRepository.save(notification, ttl);
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

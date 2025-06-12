package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.dto.ResultsPage;
import com.sojka.pomeranian.chat.model.Notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends AstraCrudRepository<Notification> {

    void deleteAllByPrimaryKeys(String profileId, List<Instant> createdAt, String senderId);

    Optional<Long> countByProfileId(String profileId);

    ResultsPage<Notification> findByProfileId(String profileId, String pageState, int pageSize);
}

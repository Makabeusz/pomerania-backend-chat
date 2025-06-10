package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.model.Notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends AstraRepository<Notification> {

    void deleteAllByPrimaryKeys(String profileId, List<Instant> createdAt, String senderId);

    Optional<Long> countByProfileId(String profileId);

//    List<Notification> findByProfileId(String profileId);
}

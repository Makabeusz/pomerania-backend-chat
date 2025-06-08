package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.model.Notification;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends AstraRepository<Notification> {

    void deleteAllByPrimaryKeys(String profileId, List<Instant> createdAt, String senderId);
}

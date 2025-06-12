package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Notification.Id> {

    Optional<Long> countByIdProfileId(String profileId);

    List<Notification> findByIdProfileId(String profileId, Pageable pageable);

//    @Query("""
//                WITH RankedNotifications AS (
//                    SELECT
//                        profile_id,
//                        sender_id,
//                        sender_username,
//                        created_at,
//                        content,
//                        ROW_NUMBER() OVER (PARTITION BY profile_id, sender_id ORDER BY created_at DESC) AS rn,
//                        COUNT(*) OVER (PARTITION BY profile_id, sender_id) AS notification_count
//                    FROM message_notifications
//                    WHERE profile_id = :profileId
//                )
//                SELECT
//                    profile_id,
//                    sender_id,
//                    sender_username,
//                    created_at,
//                    content,
//                    notification_count AS count
//                FROM RankedNotifications
//                WHERE rn = 1
//                ORDER BY created_at DESC
//            """)
//    List<NotificationHeader> findNotificationsHeaders(@Param("profileId") String profileId, Pageable pageable);

}

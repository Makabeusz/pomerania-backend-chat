package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.dto.NotificationHeader;
import com.sojka.pomeranian.chat.model.MessageNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageNotificationRepository extends JpaRepository<MessageNotification, MessageNotification.Id> {

    Optional<Long> countByIdProfileId(UUID profileId);

    Optional<Long> countByIdProfileIdAndIdSenderId(UUID profileId, UUID senderId);

    List<MessageNotification> findByIdProfileId(UUID profileId, Pageable pageable);

    @Query(value = """
            SELECT
                m.profile_id,
                m.created_at,
                m.sender_id,
                m.sender_username,
                m.content,
                (SELECT COUNT(*)
                 FROM message_notifications m2
                 WHERE m2.profile_id = m.profile_id
                 AND m2.sender_id = m.sender_id) AS notification_count
            FROM message_notifications m
            WHERE m.profile_id = :profileId
            AND m.created_at = (
                SELECT MAX(m3.created_at)
                FROM message_notifications m3
                WHERE m3.profile_id = m.profile_id
                AND m3.sender_id = m.sender_id
            )
            ORDER BY m.created_at DESC
            """, nativeQuery = true)
    List<NotificationHeader> findNotificationsHeaders(UUID profileId, Pageable pageable);

    void deleteAllByIdProfileId(UUID profileId);

    // TODO: Move image192 to User. Until then it will be here. OR if not move to user it will be sent via messages each time
    @Query(value = "SELECT image_192 from profiles WHERE id = :profileId", nativeQuery = true)
    Optional<String> findImage192(UUID profileId);

}

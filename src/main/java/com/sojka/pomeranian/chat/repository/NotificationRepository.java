package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.dto.NotificationHeader;
import com.sojka.pomeranian.chat.model.MessageNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<MessageNotification, MessageNotification.Id> {

    Optional<Long> countByIdProfileId(String profileId);

    List<MessageNotification> findByIdProfileId(String profileId, Pageable pageable);

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
    List<NotificationHeader> findNotificationsHeaders(@Param("profileId") String profileId, Pageable pageable);

}

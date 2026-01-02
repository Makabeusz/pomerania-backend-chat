package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.repository.projection.ConversationProjection;
import com.sojka.pomeranian.lib.dto.ConversationFlag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The Postgres repository exists to overcome Astra limitations.<br>
 * To achieve conversations overview the conversations need to be sorted desc by timestamp, in the same time timestamp
 * must not be part of primary (or clustering) key to be updatable.
 */
@Repository
public interface ConversationsRepository extends CrudRepository<Conversation, Conversation.Id> {

    List<Conversation> findByIdUserId(UUID userId, Pageable pageable);

    void deleteAllByIdUserId(UUID userId);

    @Query(value = """
            SELECT COUNT(*) FROM conversations c
            WHERE c.user_id = :userId
            AND c.flag = ?#{#flag.name()}""", nativeQuery = true)
    Integer countAllByIdUserIdAndFlag(UUID userId, ConversationFlag flag);

    @Query(value = """
            SELECT COUNT(*) FROM conversations c
            WHERE c.user_id = :userId
            AND (c.flag = ?#{#flag1.name()}
            OR c.flag = ?#{#flag2.name()})""", nativeQuery = true)
    Integer countAllByIdUserIdAndFlagOrFlag(UUID userId, ConversationFlag flag1, ConversationFlag flag2);

    @Query(value = """
            SELECT p.id AS recipient_id, p.username AS recipient_username, p.image_192 AS recipient_image192, \
                c.flag, c.last_message_at, c.content, c.content_type, c.unread_count
            FROM conversations c
            LEFT JOIN profiles p ON c.recipient_id = p.id
            WHERE c.user_id = :userId
            AND (c.flag = ?#{#flag1.name()}
            OR c.flag = ?#{#flag2.name()})""", nativeQuery = true)
    List<ConversationProjection> findByUserIdAndFlags(UUID userId, ConversationFlag flag1, ConversationFlag flag2, Pageable pageable);

    @Query(value = """
            SELECT p.id AS recipient_id, p.username AS recipient_username, p.image_192 AS recipient_image192, \
                c.flag, c.last_message_at, c.content, c.content_type, c.unread_count
            FROM conversations c
            LEFT JOIN profiles p ON c.recipient_id = p.id
            WHERE c.user_id = :userId
            AND c.flag = ?#{#flag.name()}""", nativeQuery = true)
    List<ConversationProjection> findByUserIdAndFlag(UUID userId, ConversationFlag flag, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE conversations
            SET unread_count = :unreadCount
            WHERE user_id = :userId AND recipient_id = :recipientId""", nativeQuery = true)
    void updateUnreadCount(UUID userId, UUID recipientId, int unreadCount);

    @Query(value = "SELECT SUM(unread_count) FROM conversations WHERE user_id = :userId", nativeQuery = true)
    Long sumUnreadCountByUserId(UUID userId);

    @Query(value = """
            SELECT unread_count FROM conversations
            WHERE user_id = :userId AND recipient_id = :recipientId""", nativeQuery = true)
    Long findUnreadCountByIdUserIdAndIdRecipientId(UUID userId, UUID recipientId);

    @Query(value = """
            SELECT p.id AS recipient_id, p.username AS recipient_username, p.image_192 AS recipient_image192, \
                null, c.last_message_at, c.content, c.content_type, c.unread_count
            FROM conversations c
            LEFT JOIN profiles p ON p.id = c.recipient_id
            WHERE c.user_id = :userId
            AND c.unread_count > 0""", nativeQuery = true)
    List<ConversationProjection> findNotifications(UUID userId, Pageable pageable);

}

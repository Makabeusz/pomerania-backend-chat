package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.dto.ConversationDto;
import com.sojka.pomeranian.chat.model.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The Postgres repository exists to overcome Astra limitations. <br>
 * To achieve conversations overview the conversations need to be sorted desc by timestamp, in the same time timestamp
 * must not be part of primary (or clustering) key to be updatable.
 */
@Repository
public interface ConversationsRepository extends CrudRepository<Conversation, Conversation.Id> {

    List<Conversation> findByIdUserId(String userId, Pageable pageable);

    void deleteAllByIdUserId(String userId);

    Optional<Long> countAllByIdUserId(String userId);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE conversations
            SET last_message_at = :timestamp
            WHERE (user_id = :userId AND recipient_id = :recipientId)
            OR (user_id = :recipientId AND recipient_id = :userId)""", nativeQuery = true)
    int updateLastMessageAt(String userId, String recipientId, Instant timestamp);

    @Query(value = """
            SELECT c.user_id, c.recipient_id, c.starred, c.last_message_at, p.image_192
            FROM conversations c
            JOIN profiles p ON c.recipient_id = p.id
            WHERE c.user_id = :userId""", nativeQuery = true)
    List<ConversationDto> findByUserIdWithRecipientImage(String userId, Pageable pageable);
}

package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.ResultsPage;
import com.sojka.pomeranian.chat.model.Message;

import java.time.Instant;

public interface MessageRepository extends AstraCrudRepository<Message> {

    ResultsPage<Message> findByRoomId(String roomId, String pageState, int pageSize);

    /**
     * Marks messages as read.
     *
     * @return The read time
     */
    Instant markRead(MessageKey key);

}

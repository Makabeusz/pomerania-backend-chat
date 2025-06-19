package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.model.Message;
import org.slf4j.Logger;

import java.time.Instant;

public interface MessageRepository extends AstraCrudRepository<Message> {

    Logger getLogger();

    ResultsPage<Message> findByRoomId(String roomId, String pageState, int pageSize);

    /**
     * Marks messages as read.
     *
     * @return The read time
     */
    Instant markRead(MessageKey key);

}

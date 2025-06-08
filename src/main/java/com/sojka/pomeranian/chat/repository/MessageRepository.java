package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessagePage;
import com.sojka.pomeranian.chat.model.Message;

import java.time.Instant;

public interface MessageRepository extends AstraRepository<Message> {

    MessagePage findByRoomId(String roomId, String pageState, int pageSize);

    /**
     * Marks messages as read.
     *
     * @return The read time
     */
    Instant markRead(MessageKey key);

}

package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.dto.MessageKey;
import com.sojka.pomeranian.chat.dto.MessagePage;
import com.sojka.pomeranian.chat.model.Message;

import java.time.Instant;
import java.util.List;

public interface MessageRepository {

    MessagePage findByRoomId(String roomId, String pageState, int pageSize);

    Message save(Message message);

    /**
     * Marks messages as read.
     *
     * @return The read time
     */
    Instant markRead(List<MessageKey> key);

}

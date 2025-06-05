package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.dto.MessagePage;
import com.sojka.pomeranian.chat.model.Message;

public interface MessageRepository {

    MessagePage findByRoomId(String roomId, String pageState, int pageSize);

    Message save(Message message);

    MessagePage findConversationsHeaders(String userId, String pageState, int pageSize);
}

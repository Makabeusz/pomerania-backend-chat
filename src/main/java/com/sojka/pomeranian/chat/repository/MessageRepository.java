package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.dto.MessagePage;
import com.sojka.pomeranian.chat.model.Message;

import java.util.List;

public interface MessageRepository {

    MessagePage findByRoomId(String roomId, String pageState);

    Message save(Message message);

    MessagePage findConversationsHeaders(String userId, String pageState);
}

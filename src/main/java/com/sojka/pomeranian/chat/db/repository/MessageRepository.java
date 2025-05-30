package com.sojka.pomeranian.chat.db.repository;

import com.sojka.pomeranian.chat.model.Message;
import reactor.core.publisher.Flux;

public interface MessageRepository {

    Flux<Message> findByRoomId(String roomId, Pageable pageable);
}

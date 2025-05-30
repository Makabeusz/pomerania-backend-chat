package com.sojka.pomeranian.chat.db.repository;

import com.sojka.pomeranian.chat.model.Message;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import reactor.core.publisher.Flux;

public class MessageRepositoryImpl implements MessageRepository {

    @Override
    public Flux<Message> findByRoomId(String roomId, Pageable pageable) {
        return null;
    }
}

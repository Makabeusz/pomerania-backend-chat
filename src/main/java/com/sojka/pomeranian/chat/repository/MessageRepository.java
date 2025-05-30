package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.model.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageRepository {

    Flux<Message> findByRoomId(String roomId, String pageState);

    Mono<Message> save(Message message);
}

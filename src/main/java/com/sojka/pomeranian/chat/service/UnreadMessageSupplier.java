package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.repository.MessageNotificationRepository;
import com.sojka.pomeranian.lib.producerconsumer.ObjectProvider;
import com.sojka.pomeranian.lib.producerconsumer.ObjectSupplier;
import com.sojka.pomeranian.lib.producerconsumer.SupplierQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static com.sojka.pomeranian.lib.util.CommonUtils.getRecipientIdFromRoomId;

@Slf4j
@Component
@Qualifier("unreadMessageSupplier")
@RequiredArgsConstructor
public class UnreadMessageSupplier extends ObjectProvider<Integer, Conversation> {

    private final MessageNotificationRepository repository;

    @Override
    protected ObjectSupplier<Integer, Conversation> getSupplier(
            SupplierQueue<Conversation> queue,
            ConcurrentSkipListMap<Integer, Pair<Conversation, Integer>> result) {
        return new UnreadMessageCountSupplier(queue, result, log);
    }

    private class UnreadMessageCountSupplier extends ObjectSupplier<Integer, Conversation> {

        public UnreadMessageCountSupplier(SupplierQueue<Conversation> queue,
                                          ConcurrentMap<Integer, Pair<Conversation, Integer>> result,
                                          Logger log) {
            super(queue, result, log);
        }

        @Override
        protected Pair<Conversation, Integer> supplyObject(Conversation conversation) {
            int count = repository.countByIdProfileIdAndIdSenderId(conversation.getId().getUserId(), conversation.getId().getRecipientId())
                    .map(Long::intValue)
                    .orElse(0);
            return Pair.of(conversation, count);
        }
    }
}

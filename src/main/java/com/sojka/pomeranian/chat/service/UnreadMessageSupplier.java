package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.ConversationDto;
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

@Slf4j
@Component
@Qualifier("unreadMessageSupplier")
@RequiredArgsConstructor
public class UnreadMessageSupplier extends ObjectProvider<Integer, ConversationDto> {

    private final MessageNotificationRepository repository;

    @Override
    protected ObjectSupplier<Integer, ConversationDto> getSupplier(
            SupplierQueue<ConversationDto> queue,
            ConcurrentSkipListMap<Integer, Pair<ConversationDto, Integer>> result) {
        return new UnreadMessageCountSupplier(queue, result, log);
    }

    private class UnreadMessageCountSupplier extends ObjectSupplier<Integer, ConversationDto> {

        public UnreadMessageCountSupplier(SupplierQueue<ConversationDto> queue,
                                          ConcurrentMap<Integer, Pair<ConversationDto, Integer>> result,
                                          Logger log) {
            super(queue, result, log);
        }

        @Override
        protected Pair<ConversationDto, Integer> supplyObject(ConversationDto conversation) {
            int count = repository.countByIdProfileIdAndIdSenderId(conversation.getUserId(), conversation.getRecipientId())
                    .map(Long::intValue)
                    .orElse(0);
            return Pair.of(conversation, count);
        }
    }
}

package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.ConversationDto;
import com.sojka.pomeranian.chat.repository.MessageNotificationRepository;
import com.sojka.pomeranian.chat.repository.MessageRepository;
import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.chat.util.mapper.MessageMapper;
import com.sojka.pomeranian.lib.producerconsumer.ObjectProvider;
import com.sojka.pomeranian.lib.producerconsumer.ObjectSupplier;
import com.sojka.pomeranian.lib.producerconsumer.SupplierQueue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@Qualifier("conversationHeadersSupplier")
@RequiredArgsConstructor
public class ConversationHeadersSupplier extends ObjectProvider<ChatMessagePersisted, ConversationDto> {

    private final MessageRepository messageRepository;
    private final MessageNotificationRepository notificationsRepository;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    protected ObjectSupplier<ChatMessagePersisted, ConversationDto> getSupplier(
            SupplierQueue<ConversationDto> queue,
            ConcurrentSkipListMap<Integer, Pair<ConversationDto, ChatMessagePersisted>> result) {
        return new UnreadMessageCountSupplier(queue, result, log);
    }

    private class UnreadMessageCountSupplier extends ObjectSupplier<ChatMessagePersisted, ConversationDto> {

        public UnreadMessageCountSupplier(
                SupplierQueue<ConversationDto> queue,
                ConcurrentMap<Integer, Pair<ConversationDto, ChatMessagePersisted>> result,
                Logger log
        ) {
            super(queue, result, log);
        }

        @Override
        protected Pair<ConversationDto, ChatMessagePersisted> supplyObject(ConversationDto conversation) {
            final MessageResult result = new MessageResult();

            executor.execute(() -> {
                ChatMessagePersisted message = null;
                try {
                    message = messageRepository.findByRoomId(CommonUtils.generateRoomId(conversation), null, 1)
                            .getResults()
                            .stream()
                            .map(MessageMapper::toDto)
                            .toList()
                            .getFirst();
                } catch (NoSuchElementException e) {
                    log.error("Empty conversation: {}", conversation);
                    message = ChatMessagePersisted.builder().build();
                }
                result.setMessage(message);
                result.setMessageOk(true);
            });
            executor.execute(() -> {
                int count = notificationsRepository.countByIdProfileIdAndIdSenderId(conversation.getUserId(), conversation.getRecipientId())
                        .map(Long::intValue)
                        .orElse(0);
                result.setUnreadCount(count);
                result.setCountOk(true);
            });

            Instant deadline = Instant.now().plusSeconds(5L);

            // TODO: refactor to a better, not busy-waiting lock
            while (!result.isReady()) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("The thread got interrupted while retrieving conversation: {}", conversation);
                    break;
                }
                if (Instant.now().isAfter(deadline)) {
                    log.error("The deadline have been reached, returning partial results");
                    break;
                }
            }
            result.getMessage().addMetadata("unread", result.getUnreadCount() + "");
            result.getMessage().addMetadata("flag", conversation.getFlag());
            result.getMessage().addMetadata("image192", conversation.getImage192() + "");

            return Pair.of(conversation, result.getMessage());
        }
    }

    @Getter
    @Setter
    private static class MessageResult {
        ChatMessagePersisted message;
        Integer unreadCount;
        boolean messageOk;
        boolean countOk;

        public MessageResult() {
            this.message = new ChatMessagePersisted();
            this.unreadCount = 0;
        }

        boolean isReady() {
            if (messageOk) {
                return countOk;
            }
            return false;
        }
    }
}

package com.sojka.pomeranian.comment.service;

import com.sojka.pomeranian.chat.util.mapper.NotificationMapper;
import com.sojka.pomeranian.lib.dto.CommentStompRequest;
import com.sojka.pomeranian.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static com.sojka.pomeranian.chat.util.Constants.COMMENTS_DESTINATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public void publish(CommentStompRequest dto) {
        log.trace("publish input: {}", dto);

        // comments section update
        messagingTemplate.convertAndSendToUser(dto.getRelatedId() + "", COMMENTS_DESTINATION, dto);
        if (dto.isPublishNotification()) {
            notificationService.publish(NotificationMapper.toDto(dto));
        }
    }
}

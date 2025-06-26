package com.sojka.pomeranian.comment.service;

import com.sojka.pomeranian.comment.CommentStompRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static com.sojka.pomeranian.chat.util.Constants.MESSAGE_DESTINATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(CommentStompRequest dto) {
        messagingTemplate.convertAndSendToUser(dto.relatedId(), MESSAGE_DESTINATION, dto);
    }
}

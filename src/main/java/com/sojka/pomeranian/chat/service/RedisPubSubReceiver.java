package com.sojka.pomeranian.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPubSubReceiver {

    private final SimpMessagingTemplate template;
    private final ObjectMapper mapper;

    public RedisPubSubReceiver(SimpMessagingTemplate template, ObjectMapper mapper) {
        this.template = template;
        this.mapper = mapper;
    }

    public void receiveMessage(String message) {
        try {
            var data = mapper.readValue(message, WebSocketMessage.class);
            template.convertAndSend(data.destination(), data.payload());
        } catch (Exception e) {
            // Log error in production
            throw new RuntimeException("Failed to forward from Redis", e);
        }
    }
}
package com.sojka.pomeranian.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "pomeranian.chat",
        name = "redis-enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class RedisPubSubReceiver {

    private final SimpMessagingTemplate template;
    private final ObjectMapper mapper;

    public void receiveMessage(String message) {
        try {
            var data = mapper.readValue(message, WebSocketMessage.class);
            template.convertAndSend(data.destination(), data.payload());
        } catch (Exception e) {
            log.error("Failed to forward WebSocket message received from Redis to local broker. " +
                            "This message will be lost for clients on this pod. rawMessagePreview={}, cause={}",
                    message != null && message.length() > 200 ? message.substring(0, 200) + "..." : message,
                    e.toString(), e);
            // Intentionally do NOT rethrow — prevents listener thread death and allows processing of subsequent messages.
        }
    }
}
package com.sojka.pomeranian.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sojka.pomeranian.chat.config.RedisPubSubConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnProperty(
        prefix = "pomeranian.chat",
        name = "redis-enabled",
        havingValue = "true"
)
public class RedisWebSocketService implements SimpMessageSendingOperations {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper;

    public RedisWebSocketService(StringRedisTemplate redisTemplate, ObjectMapper mapper) {
        this.redisTemplate = redisTemplate;
        this.mapper = mapper;
    }

    public void convertAndSendToUser(String user, String destination, Object payload) {
        String userDestination = "/user/" + user + destination;
        convertAndSend(userDestination, payload);
    }

    @Override
    public void convertAndSend(String destination, Object payload) throws MessagingException {
        try {
            var message = new WebSocketMessage<Object>(destination, payload);
            String json = mapper.writeValueAsString(message);
            redisTemplate.convertAndSend(RedisPubSubConfig.CHANNEL_NAME, json);
        } catch (Exception e) {
            // TODO: proper logging
            throw new RuntimeException("Failed to publish to Redis", e);
        }
    }

    @Override
    public void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers) throws MessagingException {
        throw new MessagingException("not implemented");
    }

    @Override
    public void convertAndSendToUser(String user, String destination, Object payload, MessagePostProcessor postProcessor) throws MessagingException {
        throw new MessagingException("not implemented");
    }

    @Override
    public void convertAndSendToUser(String user, String destination, Object payload, Map<String, Object> headers, MessagePostProcessor postProcessor) throws MessagingException {
        throw new MessagingException("not implemented");
    }

    @Override
    public void send(Message<?> message) throws MessagingException {
        throw new MessagingException("not implemented");
    }

    @Override
    public void send(String destination, Message<?> message) throws MessagingException {
        throw new MessagingException("not implemented");
    }

    @Override
    public void convertAndSend(Object payload) throws MessagingException {
        throw new MessagingException("not implemented");
    }

    @Override
    public void convertAndSend(String destination, Object payload, Map<String, Object> headers) throws MessagingException {
        throw new MessagingException("not implemented");
    }

    @Override
    public void convertAndSend(Object payload, MessagePostProcessor postProcessor) throws MessagingException {
        throw new MessagingException("not implemented");
    }

    @Override
    public void convertAndSend(String destination, Object payload, MessagePostProcessor postProcessor) throws MessagingException {
        throw new MessagingException("not implemented");
    }

    @Override
    public void convertAndSend(String destination, Object payload, Map<String, Object> headers, MessagePostProcessor postProcessor) throws MessagingException {
        throw new MessagingException("not implemented");
    }
}
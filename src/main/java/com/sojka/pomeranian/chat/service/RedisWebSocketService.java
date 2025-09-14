package com.sojka.pomeranian.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sojka.pomeranian.chat.config.RedisPubSubConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisWebSocketService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper;

    public RedisWebSocketService(StringRedisTemplate redisTemplate, ObjectMapper mapper) {
        this.redisTemplate = redisTemplate;
        this.mapper = mapper;
    }

    public <T> void convertAndSendToUser(String user, String destination, T payload) {
        String userDestination = "/user/" + user + destination;
        convertAndSend(userDestination, payload);
    }

    private <T> void convertAndSend(String destination, T payload) {
        try {
            var message = new WebSocketMessage<T>(destination, payload);
            String json = mapper.writeValueAsString(message);
            redisTemplate.convertAndSend(RedisPubSubConfig.CHANNEL_NAME, json);
        } catch (Exception e) {
            // Log error in production
            throw new RuntimeException("Failed to publish to Redis", e);
        }
    }

}
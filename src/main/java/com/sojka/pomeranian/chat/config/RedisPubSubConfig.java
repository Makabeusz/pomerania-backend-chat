package com.sojka.pomeranian.chat.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sojka.pomeranian.chat.service.RedisPubSubReceiver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.ErrorHandler;

@Slf4j
@Configuration
@ConditionalOnProperty(
        prefix = "pomeranian.chat",
        name = "redis-enabled",
        havingValue = "true"
)
public class RedisPubSubConfig {

    public static final String CHANNEL_NAME = "websocket-channel";

    /**
     * Dedicated ObjectMapper for the WebSocket Redis bridge.
     * Uses JavaTimeModule and default typing for safer cross-pod (de)serialization of payloads.
     */
    @Bean
    public ObjectMapper webSocketBridgeObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return objectMapper;
    }

    @Bean
    public RedisPubSubReceiver redisPubSubReceiver(SimpMessagingTemplate template,
                                                   @Qualifier("webSocketBridgeObjectMapper") ObjectMapper webSocketBridgeObjectMapper) {
        return new RedisPubSubReceiver(template, webSocketBridgeObjectMapper);
    }

    @Bean
    public MessageListenerAdapter redisPubSubListenerAdapter(RedisPubSubReceiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                       MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new ChannelTopic(CHANNEL_NAME));
        container.setErrorHandler(redisPubSubErrorHandler());
        return container;
    }

    @Bean
    public ErrorHandler redisPubSubErrorHandler() {
        return throwable -> {
            log.error("Error in Redis Pub/Sub WebSocket listener container: {}", throwable.getMessage(), throwable);
            // Do not rethrow — allows the container to continue processing other messages.
        };
    }
}
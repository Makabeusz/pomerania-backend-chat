package com.sojka.pomeranian.chat.config;

import com.sojka.pomeranian.chat.service.RedisPubSubReceiver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class RedisPubSubConfig {

    public static final String CHANNEL_NAME = "websocket-channel";

    @Bean
    public RedisPubSubReceiver redisPubSubReceiver(SimpMessagingTemplate template, ObjectMapper mapper) {
        return new RedisPubSubReceiver(template, mapper);
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
        return container;
    }
}
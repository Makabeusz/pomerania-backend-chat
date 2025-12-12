package com.sojka.pomeranian.chat.config.redis;

import com.sojka.pomeranian.chat.model.ActiveUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;

@Slf4j
@Configuration
public class RedisConfig {

    public static final String ACTIVE_USER_PREFIX = "active:";

    @Bean
    public RedisTemplate<UUID, ActiveUser> redisActiveUserTemplate(RedisConnectionFactory factory) {
        RedisTemplate<UUID, ActiveUser> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new UUIDRedisSerializer());
        template.setValueSerializer(new ActiveUserRedisSerializer());
        return template;
    }

}

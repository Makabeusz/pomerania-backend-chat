package com.sojka.pomeranian.chat.config.cache;

import com.sojka.pomeranian.chat.model.ActiveUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.UUID;

@Slf4j
@Configuration
public class RedisConfig {

    public static final String ACTIVE_USER_PREFIX = "active:";

    @Bean
    public RedisTemplate<UUID, ActiveUser> redisActiveUserTemplate(RedisConnectionFactory factory) {
        RedisTemplate<UUID, ActiveUser> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new UUIDRedisPrefixedSerializer(ACTIVE_USER_PREFIX));
        template.setValueSerializer(new ActiveUserRedisSerializer());
        return template;
    }

    @Bean
    public RedisTemplate<String, UUID> redisActiveUserSessionTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, UUID> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new UUIDRedisSerializer());
        return template;
    }

}

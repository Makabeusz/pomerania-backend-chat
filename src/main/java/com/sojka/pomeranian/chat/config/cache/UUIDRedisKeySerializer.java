package com.sojka.pomeranian.chat.config.cache;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UUIDRedisKeySerializer implements RedisSerializer<UUID> {

    private final String prefix;

    public UUIDRedisKeySerializer(String prefix) {
        this.prefix = prefix;
    }

    @Nullable
    @Override
    public byte[] serialize(@Nullable UUID value) {
        if (value == null) {
            return null;
        }
        String key = prefix + value;
        return key.getBytes(StandardCharsets.UTF_8);
    }

    @Nullable
    @Override
    public UUID deserialize(@Nullable byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        String fullKey = new String(bytes, StandardCharsets.UTF_8);

        if (!fullKey.startsWith(prefix)) {
            throw new SerializationException(
                    "Key does not start with expected prefix. actual=%s, expected=%s"
                            .formatted(fullKey, prefix)
            );
        }

        String uuidString = fullKey.substring(prefix.length());
        return UUID.fromString(uuidString);
    }

    @Override
    public Class<?> getTargetType() {
        return UUID.class;
    }
}
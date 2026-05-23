package com.sojka.pomeranian.chat.config.cache;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;

public class StringRedisKeySerializer implements RedisSerializer<String> {

    private final String prefix;

    public StringRedisKeySerializer(String prefix) {
        this.prefix = prefix;
    }

    @Nullable
    @Override
    public byte[] serialize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String key = prefix + value;
        return key.getBytes(StandardCharsets.UTF_8);
    }

    @Nullable
    @Override
    public String deserialize(@Nullable byte[] bytes) {
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

        return fullKey.substring(prefix.length());
    }

    @Override
    public Class<?> getTargetType() {
        return String.class;
    }
}
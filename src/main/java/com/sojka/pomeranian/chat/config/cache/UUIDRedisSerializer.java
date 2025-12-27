package com.sojka.pomeranian.chat.config.cache;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDRedisSerializer implements RedisSerializer<UUID> {

    private static final int UUID_LEN = 16;

    @Nullable
    @Override
    public byte[] serialize(@Nullable UUID value) {
        if (value == null) {
            return null;
        } else {
            ByteBuffer bb = ByteBuffer.allocate(UUID_LEN);
            bb.putLong(value.getMostSignificantBits());
            bb.putLong(value.getLeastSignificantBits());
            return bb.array();
        }
    }

    @Nullable
    @Override
    public UUID deserialize(@Nullable byte[] bytes) {
        if (bytes == null || bytes.length < UUID_LEN) {
            return null;
        } else {
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            return new UUID(bb.getLong(), bb.getLong());
        }
    }

    @Override
    public Class<?> getTargetType() {
        return UUID.class;
    }
}

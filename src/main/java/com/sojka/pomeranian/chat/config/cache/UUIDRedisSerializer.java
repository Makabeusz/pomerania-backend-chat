package com.sojka.pomeranian.chat.config.cache;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;
import java.util.UUID;

import static com.sojka.pomeranian.chat.config.cache.RedisConfig.ACTIVE_USER_PREFIX;
import static java.nio.charset.StandardCharsets.UTF_8;

public class UUIDRedisSerializer implements RedisSerializer<UUID> {

    private static final byte[] PREFIX_BYTES = ACTIVE_USER_PREFIX.getBytes(UTF_8);
    private static final int PREFIX_LEN = 7;
    private static final int UUID_LEN = 16;
    private static final int KEY_LENGTH = 23; // UUID 16 bytes + 'active:' 7 bytes = 23 bytes

    @Nullable
    @Override
    public byte[] serialize(@Nullable UUID value) {
        if (value == null) {
            return null;
        } else {
            ByteBuffer bb = ByteBuffer.allocate(KEY_LENGTH);
            bb.put(PREFIX_BYTES);
            bb.putLong(value.getMostSignificantBits());
            bb.putLong(value.getLeastSignificantBits());
            return bb.array();
        }
    }

    @Nullable
    @Override
    public UUID deserialize(@Nullable byte[] bytes) {
        if (bytes == null || bytes.length < PREFIX_LEN + UUID_LEN) {
            return null;
        } else {
            // Check prefix
            for (int i = 0; i < PREFIX_LEN; i++) {
                if (bytes[i] != PREFIX_BYTES[i]) {
                    throw new SerializationException("Key does not start with expected prefix. actual=%s, expected=%s"
                            .formatted(new String(bytes, 0, PREFIX_LEN, UTF_8), ACTIVE_USER_PREFIX)
                    );
                }
            }
            // Extract UUID bytes
            ByteBuffer bb = ByteBuffer.wrap(bytes, PREFIX_LEN, UUID_LEN);
            return new UUID(bb.getLong(), bb.getLong());
        }
    }

    @Override
    public Class<?> getTargetType() {
        return UUID.class;
    }
}

package com.sojka.pomeranian.chat.config.redis;

import com.sojka.pomeranian.chat.model.ActiveUser;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ActiveUserRedisSerializer implements RedisSerializer<ActiveUser> {

    @Nullable
    @Override
    public byte[] serialize(@Nullable ActiveUser value) {
        if (value == null) {
            return null;
        } else {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try (DataOutputStream dos = new DataOutputStream(stream)) {
                // UUID userId
                dos.writeLong(value.getUserId().getMostSignificantBits());
                dos.writeLong(value.getUserId().getLeastSignificantBits());
                // Map<String, List<String>> subscriptions
                Map<String, List<String>> subs = value.getSubscriptions();
                dos.writeInt(subs.size());
                for (Map.Entry<String, List<String>> entry : subs.entrySet()) {
                    dos.writeUTF(entry.getKey());
                    List<String> list = entry.getValue();
                    dos.writeInt(list.size());
                    for (String s : list) {
                        dos.writeUTF(s);
                    }
                }
                // String simpSessionId
                dos.writeUTF(value.getSimpSessionId());
                // Instant createdAt
                dos.writeLong(value.getCreatedAt().getEpochSecond());
                dos.writeInt(value.getCreatedAt().getNano());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return stream.toByteArray();
        }
    }

    @Nullable
    @Override
    public ActiveUser deserialize(@Nullable byte[] bytes) {
        if (bytes == null) {
            return null;
        } else {
            try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes))) {
                // UUID userId
                UUID userId = new UUID(dis.readLong(), dis.readLong());
                // Map<String, List<String>> subscriptions
                int mapSize = dis.readInt();
                Map<String, List<String>> subs = new HashMap<>(mapSize);
                for (int i = 0; i < mapSize; i++) {
                    String key = dis.readUTF();
                    int listSize = dis.readInt();
                    List<String> list = new ArrayList<>(listSize);
                    for (int j = 0; j < listSize; j++) {
                        list.add(dis.readUTF());
                    }
                    subs.put(key, list);
                }
                // String simpSessionId
                String simpSessionId = dis.readUTF();
                // Instant createdAt
                Instant createdAt = Instant.ofEpochSecond(dis.readLong(), dis.readInt());
                return new ActiveUser(userId, subs, simpSessionId, createdAt);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Class<?> getTargetType() {
        return ActiveUser.class;
    }
}

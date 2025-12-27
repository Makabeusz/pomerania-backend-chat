package com.sojka.pomeranian.chat.config.cache;

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
                // List<Session> sessions
                List<ActiveUser.Session> sessions = value.getSessions();
                dos.writeInt(sessions.size());
                for (ActiveUser.Session session : sessions) {
                    // Map<String, List<String>> subscriptions
                    Map<String, List<String>> subs = session.getSubscriptions();
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
                    dos.writeUTF(session.getSimpSessionId());
                    // Instant createdAt
                    dos.writeLong(session.getCreatedAt().getEpochSecond());
                    dos.writeInt(session.getCreatedAt().getNano());
                }
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
                // List<Session> sessions
                int listSize = dis.readInt();
                List<ActiveUser.Session> sessions = new ArrayList<>(listSize);
                for (int i = 0; i < listSize; i++) {
                    // Map<String, List<String>> subscriptions
                    int mapSize = dis.readInt();
                    Map<String, List<String>> subs = new HashMap<>(mapSize);
                    for (int j = 0; j < mapSize; j++) {
                        String key = dis.readUTF();
                        int subListSize = dis.readInt();
                        List<String> list = new ArrayList<>(subListSize);
                        for (int k = 0; k < subListSize; k++) {
                            list.add(dis.readUTF());
                        }
                        subs.put(key, list);
                    }
                    // String simpSessionId
                    String simpSessionId = dis.readUTF();
                    // Instant createdAt
                    Instant createdAt = Instant.ofEpochSecond(dis.readLong(), dis.readInt());
                    sessions.add(new ActiveUser.Session(subs, simpSessionId, createdAt));
                }
                return new ActiveUser(userId, sessions);
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
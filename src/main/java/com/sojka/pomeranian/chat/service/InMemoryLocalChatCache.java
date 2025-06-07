package com.sojka.pomeranian.chat.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryLocalChatCache implements ChatCache {

    private final Set<String> activeUsers = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isOnline(String userId) {
        return activeUsers.contains(userId);
    }

    @Override
    public boolean put(String userId) {
        return activeUsers.add(userId);
    }

    @Override
    public boolean remove(String userId) {
        return activeUsers.remove(userId);
    }
}

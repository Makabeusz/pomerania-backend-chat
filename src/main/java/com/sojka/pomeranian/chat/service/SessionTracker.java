package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTracker {

    private final ChatCache cache;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        User user = CommonUtils.getAuthUser(event.getUser());

        boolean failed = !cache.put(user.getId());

        if (failed) {
            log.error("Failed to put cache entry, user_id={}", user.getId());
        } else {
            log.info("Connected to the chat: {}", user.getId());
        }
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        User user = CommonUtils.getAuthUser(event.getUser());
        boolean failed = !cache.remove(user.getId());

        if (failed) {
            log.error("Failed to remove cache entry, user_id={}", user.getId());
        } else {
            log.info("Disconnected from a chat: {}", user.getId());
        }
    }

    public boolean isUserOnline(String userId) {
        return cache.isOnline(userId);
    }
}
package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.config.StompRequestAuthenticator;
import com.sojka.pomeranian.chat.service.cache.ChatCache;
import com.sojka.pomeranian.lib.dto.UserPresenceRequest;
import com.sojka.pomeranian.lib.util.DateTimeUtils;
import com.sojka.pomeranian.pubsub.UserPresencePublisher;
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
    private final UserPresencePublisher publisher;
    private final StompRequestAuthenticator requestAuthenticator;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        String simpSessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        User user = requestAuthenticator.getUser(event);
        cache.create(user.getId(), simpSessionId);
        publisher.publish(new UserPresenceRequest(user.getId(), true, DateTimeUtils.getCurrentInstant()));
        log.info("Online: user_id={}", user.getId());
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        User user = requestAuthenticator.getUser(event);
        cache.remove(user.getId());
        publisher.publish(new UserPresenceRequest(user.getId(), false, DateTimeUtils.getCurrentInstant()));
        log.info("Offline: user_id={}", user.getId());
    }

}
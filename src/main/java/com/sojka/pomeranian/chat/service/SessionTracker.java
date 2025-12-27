package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.config.StompRequestAuthenticator;
import com.sojka.pomeranian.chat.service.cache.SessionCache;
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

    private final SessionCache cache;
    private final UserPresencePublisher publisher;
    private final StompRequestAuthenticator requestAuthenticator;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        log.trace("Session connect attempt");
        try {
            String simpSessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
            User user = requestAuthenticator.getUser(event);
            boolean isCreated = cache.create(user.getId(), simpSessionId);
            if (isCreated) {
                publisher.publish(new UserPresenceRequest(user.getId(), true, DateTimeUtils.getCurrentInstant()));
                log.debug("Online: userId={}", user.getId());
            } else {
                log.debug("Already online: userId={}", user.getId());
            }
        } catch (Exception e) {
            log.debug("Connect attempt without auth", e);
        }
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        try {
            var userId = cache.remove(event.getSessionId());
            publisher.publish(new UserPresenceRequest(userId, false, DateTimeUtils.getCurrentInstant()));
            log.debug("Offline: userId={}", userId);
        } catch (Exception e) {
            log.debug("Unknown session", e);
        }
    }

}
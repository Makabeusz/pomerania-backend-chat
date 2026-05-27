package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.config.StompRequestAuthenticator;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.cache.SessionCache;
import com.sojka.pomeranian.lib.util.DateTimeUtils;
import com.sojka.pomeranian.security.model.Role;
import com.sojka.pomeranian.security.model.User;
import com.sojka.pomeranian.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTracker {

    private final SessionCache cache;
    private final StompRequestAuthenticator authenticator;
    private final UserRepository userRepository;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        log.trace("SessionConnectedEvent: {}", event);
        try {
            String simpSessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
            User user = authenticator.getUser(event);
            boolean isCreated = cache.create(user.getId(), simpSessionId);
            if (isCreated) {
                userRepository.updateLastLoginAtAndIsOnline(user.getId(), DateTimeUtils.getCurrentInstant(), true);
                log.debug("Online: userId={}", user.getId());
            } else {
                log.debug("Already online: userId={}", user.getId());
            }
        } catch (SecurityException e) {
            log.debug("Connect attempt without auth", e);
        } catch (Exception e) {
            log.error("Unexpected error handling SessionConnectedEvent", e);
        }
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        log.trace("SessionDisconnectEvent: {}", event);
        try {
            var userId = cache.remove(event.getSessionId());
            if (userId != null) {
                userRepository.updateLastLoginAtAndIsOnline(userId, DateTimeUtils.getCurrentInstant(), false);
                log.debug("Offline: userId={}", userId);
            } else {
                log.debug("SessionId={} already removed or never tracked (user may still have other sessions)", event.getSessionId());
            }
        } catch (SecurityException e) {
            log.debug("Unknown session", e);
        } catch (Exception e) {
            log.error("Unexpected error handling SessionDisconnectEvent", e);
        }
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        log.trace("SessionUnsubscribeEvent: {}", event);
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            User user = authenticator.getUser(event, headerAccessor);
            StompSubscription subscription = getSubscriptionFromHeaders(headerAccessor);
            if (subscription != null) {
                cache.remove(user.getId(), headerAccessor.getSessionId(), subscription);
                log.debug(
                        "Unsubscribed: user={}, simpSessionId={}, subscription={}",
                        user.getUsername(), headerAccessor.getSessionId(), subscription
                );
            } else {
                log.debug("Unsubscribe without subType header: user={}, simpSessionId={}",
                        user.getUsername(), headerAccessor.getSessionId());
            }
        } catch (SecurityException e) {
            log.debug("Unknown session", e);
        } catch (Exception e) {
            log.error("Unexpected error handling SessionUnsubscribeEvent", e);
        }
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        log.trace("SessionSubscribeEvent: {}", event);
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            User user = authenticator.getUser(event, headerAccessor);
            StompSubscription subscription = getSubscriptionFromHeaders(headerAccessor);

            if (subscription == null) {
                log.debug("Subscribe without subType header: user={}, simpSessionId={}",
                        user.getUsername(), headerAccessor.getSessionId());
            } else if (user.getRole() != Role.PomeranianRole.DEACTIVATED) {
                cache.add(user.getId(), headerAccessor.getSessionId(), subscription);
                log.debug("Subscribed: user_id={}, simpSessionId={}, subscription={}",
                        user.getId(), headerAccessor.getSessionId(), subscription);
            } else {
                log.debug("User={} deactivated, can't subscribe", user.getUsername());
            }
        } catch (SecurityException e) {
            log.debug("Unknown session", e);
        } catch (Exception e) {
            log.error("Unexpected error handling SessionSubscribeEvent", e);
        }
    }

    StompSubscription getSubscriptionFromHeaders(StompHeaderAccessor headerAccessor) {
        String typeName = getNativeHeader(headerAccessor, "subType");
        if (typeName != null && !typeName.isEmpty()) {
            StompSubscription.Type type = StompSubscription.Type.valueOf(typeName);
            String id = getNativeHeader(headerAccessor, "subId");
            return new StompSubscription(type, id);
        }
        return null;
    }

    String getNativeHeader(StompHeaderAccessor headerAccessor, String name) {
        List<String> headers = headerAccessor.getNativeHeader(name);
        return getHeaderValue(headers);
    }

    String getHeaderValue(List<String> headers, String defaultValue) {
        return (headers != null && !headers.isEmpty()) ? headers.getFirst() : defaultValue;
    }

    String getHeaderValue(List<String> headers) {
        return getHeaderValue(headers, null);
    }

}
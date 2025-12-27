package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.config.StompRequestAuthenticator;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.cache.SessionCache;
import com.sojka.pomeranian.lib.dto.UserPresenceRequest;
import com.sojka.pomeranian.lib.util.DateTimeUtils;
import com.sojka.pomeranian.pubsub.UserPresencePublisher;
import com.sojka.pomeranian.security.model.Role;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTracker {

    private static final String SUB_SIZE_KEY = "subSize";
    private static final String SUB_TYPE_KEY = "subType";
    private static final String SUB_ID_KEY = "subId";

    private final SessionCache cache;
    private final UserPresencePublisher publisher;
    private final StompRequestAuthenticator requestAuthenticator;
    private final StompRequestAuthenticator authenticator;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        log.trace("SessionConnectedEvent: {}", event);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, List<String>> nativeHeaders = getNativeHeaders(accessor);

        try {
            String simpSessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
            User user = requestAuthenticator.getUser(event, accessor);
            boolean isCreated = cache.create(user.getId(), simpSessionId);
            int subSizeKey = Integer.parseInt(getHeaderValue(nativeHeaders.get(SUB_SIZE_KEY), "0"));
            if (subSizeKey > 0) {
                List<StompSubscription> subscriptions = new ArrayList<>(subSizeKey);
                for (int i = 0; i < subSizeKey; i++) {
                    String id = getHeaderValue(nativeHeaders.get(SUB_ID_KEY + i));
                    String type = getHeaderValue(nativeHeaders.get(SUB_TYPE_KEY + i));
                    if (id == null || type == null) {
                        log.error(
                                "Session connection attempt with null subscription values: user={}, id={}, type={}",
                                user.getUsername(), id, type
                        );
                    } else {
                        subscriptions.add(new StompSubscription(StompSubscription.Type.valueOf(type), id));
                    }
                }
                if (!subscriptions.isEmpty()) {
                    cache.add(user.getId(), simpSessionId, subscriptions);
                } else {
                    log.error("No subscription had a valid values: user={}, subSize={}", user.getUsername(), subSizeKey);
                }
            }

            if (isCreated) {
                publisher.publish(new UserPresenceRequest(user.getId(), true, DateTimeUtils.getCurrentInstant()));
                log.debug("Online: userId={}", user.getId());
            } else {
                log.debug("Already online: userId={}", user.getId());
            }
        } catch (SecurityException e) {
            log.debug("Connect attempt without auth", e);
        }
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        log.trace("SessionDisconnectEvent: {}", event);
        try {
            var userId = cache.remove(event.getSessionId());
            publisher.publish(new UserPresenceRequest(userId, false, DateTimeUtils.getCurrentInstant()));
            log.debug("Offline: userId={}", userId);
        } catch (SecurityException e) {
            log.debug("Unknown session", e);
        }
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        log.trace("SessionUnsubscribeEvent: {}", event);
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            User user = authenticator.getUser(event, headerAccessor);
            StompSubscription subscription = getSubscriptionFromHeaders(headerAccessor);
            cache.remove(user.getId(), headerAccessor.getSessionId(), subscription);
            log.debug(
                    "Unsubscribed: user={}, simpSessionId={}, subscription={}",
                    user.getUsername(), headerAccessor.getSessionId(), subscription
            );
        } catch (SecurityException e) {
            log.debug("Unknown session", e);
        }
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        log.trace("SessionSubscribeEvent: {}", event);
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            User user = authenticator.getUser(event, headerAccessor);
            StompSubscription subscription = getSubscriptionFromHeaders(headerAccessor);

            if (user.getRole() != Role.PomeranianRole.DEACTIVATED) {
                cache.add(user.getId(), headerAccessor.getSessionId(), subscription);
                log.debug("Subscribed: user_id={}, simpSessionId={}, subscription={}",
                        user.getId(), headerAccessor.getSessionId(), subscription);
            } else {
                log.debug("User={} deactivated, can't subscribe", user.getUsername());
            }
        } catch (SecurityException e) {
            log.debug("Unknown session", e);
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

    // TODO: reuse in StompRequestAuthenticator#getAuthJwt
    private Map<String, List<String>> getNativeHeaders(StompHeaderAccessor accessor) {
        Object simpConnectMessage = accessor.getMessageHeaders().get("simpConnectMessage");
        if (simpConnectMessage instanceof GenericMessage simpConnectMessageHeader) {
            var nativeHeaders = simpConnectMessageHeader.getHeaders().get("nativeHeaders");
            if (nativeHeaders instanceof Map nativeHeadersMap) {
                return nativeHeadersMap;
            }
        }
        return Collections.emptyMap();
    }

}
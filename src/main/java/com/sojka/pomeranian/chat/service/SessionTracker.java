package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.util.CommonUtils;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTracker {

    private final ChatCache cache;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        String connector = getConnectorHeaderValue(event);

        if ("chat".equals(connector)) {
            User user = CommonUtils.getAuthUser(event.getUser());
            cache.put(user.getId());
        }

    }

    public boolean isUserOnline(String userId) {
        return cache.isOnline(userId);
    }

    String getConnectorHeaderValue(SessionConnectedEvent event) {
        try {
            GenericMessage<?> genericMessage = (GenericMessage<?>) event.getMessage().getHeaders().get("simpConnectMessage");
            Map nativeHeaders = (Map) genericMessage.getHeaders().get("nativeHeaders");
            List connectorList = (List) nativeHeaders.get("connector");
            return (String) connectorList.getFirst();
        } catch (Exception e) {
            log.error("Can't recognise socket source", e);
        }
        return null;
    }
}
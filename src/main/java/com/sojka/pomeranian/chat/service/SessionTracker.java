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
        String simpSessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        User user = CommonUtils.getAuthUser(event.getUser());
        cache.create(user.getId(), simpSessionId);
        log.info("Online: user_id={}", user.getId());
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        User user = CommonUtils.getAuthUser(event.getUser());
        cache.remove(user.getId());
        log.info("Offline: user_id={}", user.getId());
    }

//    String getConnectorHeaderValue(SessionConnectedEvent event) {
//        try {
//            GenericMessage<?> genericMessage = (GenericMessage<?>) event.getMessage().getHeaders().get("simpConnectMessage");
//            Map nativeHeaders = (Map) genericMessage.getHeaders().get("nativeHeaders");
//            List connectorList = (List) nativeHeaders.get("connector");
//            return (String) connectorList.getFirst();
//        } catch (Exception e) {
//            log.error("Can't recognise socket source", e);
//        }
//        return null;
//    }
}
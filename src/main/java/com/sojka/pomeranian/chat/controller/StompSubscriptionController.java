package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.config.StompRequestAuthenticator;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.cache.SessionCache;
import com.sojka.pomeranian.security.model.Role;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StompSubscriptionController {

    private final SessionCache cache;
    private final StompRequestAuthenticator authenticator;

    @MessageMapping("/unsubscribe")
    public void unsubscribe(
            @Payload List<StompSubscription> subscriptions,
            StompHeaderAccessor headerAccessor
    ) {
        User user = authenticator.getUser(headerAccessor);
        cache.remove(user.getId(), headerAccessor.getSessionId(), subscriptions);
        log.debug(
                "Unsubscribed: user={}, simpSessionId={}, subscription={}",
                user.getId(), headerAccessor.getSessionId(), subscriptions
        );
    }

    @MessageMapping("/subscribe")
    public void subscribe(
            @Payload StompSubscription subscription,
            StompHeaderAccessor headerAccessor
    ) {
        User user = authenticator.getUser(headerAccessor);
        if (user.getRole() != Role.PomeranianRole.DEACTIVATED) {
            cache.add(user.getId(), headerAccessor.getSessionId(), subscription);
            log.debug(
                    "Subscribed: user_id={}, simpSessionId={}, subscription={}",
                    user.getId(), headerAccessor.getSessionId(), subscription
            );
        }
    }

}

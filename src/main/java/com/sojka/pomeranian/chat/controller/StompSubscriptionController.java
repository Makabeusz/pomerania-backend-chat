package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.ChatCache;
import com.sojka.pomeranian.security.model.Role;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static com.sojka.pomeranian.lib.util.CommonUtils.getAuthUser;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StompSubscriptionController {

    private final ChatCache cache;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/unsubscribe")
    public void unsubscribe(@Payload List<StompSubscription> subscriptions,
                            Principal principal) {
        removeFromCache(getAuthUser(principal).getId(), subscriptions);

    }

    @MessageMapping("/subscribe")
    public void subscribe(@Payload StompSubscription subscription,
                          Principal principal) {
        User user = getAuthUser(principal);
        if (user.getRole() != Role.PomeranianRole.DEACTIVATED) {
            cache.put(user.getId(), subscription);
            log.debug("Subscribed: user_id={}, subscription={}", user.getId(), subscription);
        }
    }

    void removeFromCache(UUID userId, List<StompSubscription> connectors) {
        cache.remove(userId, connectors);
        log.debug("Unsubscribed: user={}, subscription={}", userId, connectors);
    }
}

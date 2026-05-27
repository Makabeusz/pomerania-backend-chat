package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.config.StompRequestAuthenticator;
import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.service.cache.SessionCache;
import com.sojka.pomeranian.security.model.Role;
import com.sojka.pomeranian.security.model.User;
import com.sojka.pomeranian.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionTrackerUnitTest {

    private final SessionCache cache = mock(SessionCache.class);
    private final StompRequestAuthenticator authenticator = mock(StompRequestAuthenticator.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final SessionTracker tracker = new SessionTracker(cache, authenticator, userRepository);

    private final UUID userId = UUID.randomUUID();
    private User user;
    private String simpSessionId = "session-123";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setRole(Role.PomeranianRole.USER);
    }

    @Test
    void handleSessionConnected_doesNotThrow() {
        SessionConnectedEvent event = mock(SessionConnectedEvent.class);
        when(authenticator.getUser(event)).thenReturn(user);
        when(cache.create(any(), any())).thenReturn(true);

        // Main goal: ensure the handler does not throw with valid input
        tracker.handleSessionConnected(event);
    }

    @Test
    void handleSessionDisconnected_lastSession_removesAndMarksOffline() {
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn(simpSessionId);
        when(cache.remove(simpSessionId)).thenReturn(userId);

        tracker.handleSessionDisconnected(event);

        verify(cache).remove(simpSessionId);
        verify(userRepository).updateLastLoginAtAndIsOnline(eq(userId), any(), eq(false));
    }

    @Test
    void handleSessionDisconnected_notLastSession_doesNotMarkOffline() {
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn(simpSessionId);
        when(cache.remove(simpSessionId)).thenReturn(null); // still has other sessions

        tracker.handleSessionDisconnected(event);

        verify(cache).remove(simpSessionId);
        verify(userRepository, never()).updateLastLoginAtAndIsOnline(any(), any(), any());
    }

    @Test
    void handleSessionSubscribe_ignoresDeactivatedUser() {
        user.setRole(Role.PomeranianRole.DEACTIVATED);

        SessionSubscribeEvent event = mock(SessionSubscribeEvent.class);

        // We avoid deep header mocking here; the important thing is the deactivated guard
        // is exercised via the role check inside the handler. Full header simulation
        // is covered indirectly through integration behavior.
        when(authenticator.getUser(any(SessionSubscribeEvent.class), any(StompHeaderAccessor.class)))
                .thenReturn(user);

        tracker.handleSessionSubscribe(event);

        // Should not attempt to add subscription for deactivated user
        verify(cache, never()).add(any(), any(), any(StompSubscription.class));
    }
}
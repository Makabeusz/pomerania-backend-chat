package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.exception.CacheException;
import com.sojka.pomeranian.chat.model.ActiveUser;
import com.sojka.pomeranian.chat.service.cache.InMemoryLocalSessionCache;
import com.sojka.pomeranian.chat.service.cache.SessionCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLocalSessionCacheUnitTest {

    Map<UUID, ActiveUser> db = new HashMap<>();
    Map<String, UUID> sessions = new HashMap<>();
    SessionCache cache = new InMemoryLocalSessionCache(db, sessions);

    @BeforeEach
    void setUp() {
        db.clear();
        sessions.clear();
    }

    UUID userId = UUID.randomUUID();

    @Test
    void add_newEntry_true() {
        String simpSessionId = "session1";
        cache.create(userId, simpSessionId);

        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, userId.toString());

        assertTrue(cache.add(userId, simpSessionId, subscription));
        assertTrue(db.containsKey(userId));
    }

    @Test
    void add_existingEntry_false() {
        String simpSessionId = "session1";
        cache.create(userId, simpSessionId);

        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, userId.toString());
        cache.add(userId, simpSessionId, subscription);

        assertFalse(cache.add(userId, simpSessionId, subscription));
        assertTrue(db.containsKey(userId));
    }

    @Test
    void isOnline_userPresent_true() {
        String simpSessionId = "session1";
        cache.create(userId, simpSessionId);

        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, userId.toString());
        cache.add(userId, simpSessionId, subscription);

        assertTrue(cache.isOnline(userId, subscription));
    }

    @Test
    void isOnline_userAbsent_false() {
        assertFalse(cache.isOnline(userId, new StompSubscription(StompSubscription.Type.CHAT, userId.toString())));
    }

    @Test
    void remove_existingUser_true() {
        String simpSessionId = "session1";
        cache.create(userId, simpSessionId);

        assertNotNull(cache.remove(simpSessionId));
        assertFalse(db.containsKey(userId));
    }

    @Test
    void remove_nonExistingUser_throws() {
        String simpSessionId = "session1";

        assertThatThrownBy(() -> cache.remove(simpSessionId))
                .isInstanceOf(CacheException.class)
                .hasMessage("Session=%s not online".formatted(simpSessionId));
    }

    @Test
    void purge_nonEmptyCache_clearsAll() {
        String simpSessionId1 = "session1";
        cache.create(userId, simpSessionId1);

        UUID userId2 = UUID.randomUUID();
        String simpSessionId2 = "session2";
        cache.create(userId2, simpSessionId2);

        cache.purge();
        assertTrue(db.isEmpty());
    }

    @Test
    void purge_emptyCache_noEffect() {
        cache.purge();
        assertTrue(db.isEmpty());
    }

    @Test
    void isOnline_userPresentNoSubscriptionType_false() {
        String simpSessionId = "session1";
        cache.create(userId, simpSessionId);

        assertFalse(cache.isOnline(userId, new StompSubscription(StompSubscription.Type.CHAT, "sub1")));
    }

    @Test
    void isOnline_userPresentNoMatchingSubscriptionId_false() {
        String simpSessionId = "session1";
        cache.create(userId, simpSessionId);

        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, "sub1");
        cache.add(userId, simpSessionId, subscription);

        assertFalse(cache.isOnline(userId, new StompSubscription(StompSubscription.Type.CHAT, "sub2")));
    }

    @Test
    void get_userPresent_returnsUser() {
        String simpSessionId = "session1";
        cache.create(userId, simpSessionId);

        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, "sub1");
        cache.add(userId, simpSessionId, subscription);

        assertTrue(cache.get(userId).isPresent());
    }

    @Test
    void get_userAbsent_returnsEmpty() {
        assertFalse(cache.get(userId).isPresent());
    }

    @Test
    void getAll_emptyCache_returnsEmptyList() {
        List<ActiveUser> result = cache.getAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAll_multipleUsers_returnsAllUsers() {
        String simpSessionId1 = "session1";
        cache.create(userId, simpSessionId1);

        UUID userId2 = UUID.randomUUID();
        String simpSessionId2 = "session2";
        cache.create(userId2, simpSessionId2);

        List<ActiveUser> result = cache.getAll();

        assertEquals(2, result.size());
    }

    @Test
    void add_userAbsent_throws() {
        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, "sub1");

        assertThatThrownBy(() -> cache.add(userId, "session1", subscription))
                .isInstanceOf(CacheException.class)
                .hasMessage("User=%s is not online".formatted(userId));
    }

    @Test
    void add_newSubscriptionType_true() {
        String simpSessionId = "session1";
        cache.create(userId, simpSessionId);

        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT_NOTIFICATIONS, "sub1");

        assertTrue(cache.add(userId, simpSessionId, subscription));
    }

    @Test
    void create_newUser_true() {
        String sessionId = "session1";

        assertTrue(cache.create(userId, sessionId));
        assertTrue(db.containsKey(userId));
    }

    @Test
    void create_existingUser_addsSession_true() {
        String sessionId1 = "session1";
        cache.create(userId, sessionId1);

        String sessionId2 = "session2";

        assertTrue(cache.create(userId, sessionId2));
        assertEquals(2, db.get(userId).getSessions().size());
    }

    @Test
    void create_duplicateSession_throws() {
        String sessionId = "session1";
        cache.create(userId, sessionId);

        assertThatThrownBy(() -> cache.create(userId, sessionId))
                .isInstanceOf(CacheException.class)
                .hasMessage("User=%s, simpSessionId=%s already online".formatted(userId, sessionId));
    }

    @Test
    void add_nonExistingSession_throws() {
        String sessionId = "session1";
        cache.create(userId, sessionId);

        String wrongSessionId = "wrong";
        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, "sub1");

        assertThatThrownBy(() -> cache.add(userId, wrongSessionId, subscription))
                .isInstanceOf(CacheException.class)
                .hasMessage("User=%s do not have active simpSessionID=%s".formatted(userId, wrongSessionId));
    }

    @Test
    void remove_oneSession_removesWholeUser() {
        String sessionId1 = "session1";
        cache.create(userId, sessionId1);

        String sessionId2 = "session2";
        cache.create(userId, sessionId2);

        assertNotNull(cache.remove(sessionId1));
        assertFalse(db.containsKey(userId));
    }
}
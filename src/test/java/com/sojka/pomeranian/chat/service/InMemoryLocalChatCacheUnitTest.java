package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.model.ActiveUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLocalChatCacheUnitTest {

    Map<String, ActiveUser> db = new HashMap<>();
    ChatCache cache = new InMemoryLocalChatCache(db);

    @BeforeEach
    void setUp() {
        db.clear();
    }

    @Test
    void put_newEntry_true() {
        String userId = "user1";

        assertTrue(cache.put(userId, StompSubscription.CHAT));
        assertTrue(db.containsKey(userId));
    }

    @Test
    void put_existingEntry_false() {
        String userId = "user1";
        db.put(userId, new ActiveUser(userId, Set.of(StompSubscription.CHAT), null));

        assertFalse(cache.put(userId, StompSubscription.CHAT));
        assertTrue(db.containsKey(userId));
    }

    @Test
    void isOnline_userPresent_true() {
        String userId = "user1";
        db.put(userId, new ActiveUser(userId, Set.of(StompSubscription.CHAT), null));

        assertTrue(cache.isOnline(userId, StompSubscription.CHAT));
    }

    @Test
    void isOnline_userAbsent_false() {
        String userId = "user1";

        assertFalse(cache.isOnline(userId, StompSubscription.CHAT));
    }

    @Test
    void remove_existingUser_true() {
        String userId = "user1";
        db.put(userId, new ActiveUser(userId, Set.of(StompSubscription.CHAT), null));

        assertTrue(cache.remove(userId));
        assertFalse(db.containsKey(userId));
    }

    @Test
    void remove_nonExistingUser_false() {
        String userId = "user1";

        assertFalse(cache.remove(userId));
        assertFalse(db.containsKey(userId));
    }

    @Test
    void purge_nonEmptyCache_clearsAll() {
        db.put("user1", new ActiveUser());
        db.put("user2", new ActiveUser());

        cache.purge();
        assertTrue(db.isEmpty());
    }

    @Test
    void purge_emptyCache_noEffect() {
        cache.purge();
        assertTrue(db.isEmpty());
    }
}
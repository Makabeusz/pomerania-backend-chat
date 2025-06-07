package com.sojka.pomeranian.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLocalChatCacheUnitTest {

    Set<String> db = ConcurrentHashMap.newKeySet();
    ChatCache cache = new InMemoryLocalChatCache(db);

    @BeforeEach
    void setUp() {
        db.clear();
    }

    @Test
    void put_newEntry_true() {
        String userId = "user1";

        assertTrue(cache.put(userId));
        assertTrue(db.contains(userId));
    }

    @Test
    void put_existingEntry_false() {
        String userId = "user1";
        db.add(userId);

        assertFalse(cache.put(userId));
        assertTrue(db.contains(userId));
    }

    @Test
    void isOnline_userPresent_true() {
        String userId = "user1";
        db.add(userId);

        assertTrue(cache.isOnline(userId));
    }

    @Test
    void isOnline_userAbsent_false() {
        String userId = "user1";

        assertFalse(cache.isOnline(userId));
    }

    @Test
    void remove_existingUser_true() {
        String userId = "user1";
        db.add(userId);

        assertTrue(cache.remove(userId));
        assertFalse(db.contains(userId));
    }

    @Test
    void remove_nonExistingUser_false() {
        String userId = "user1";

        assertFalse(cache.remove(userId));
        assertFalse(db.contains(userId));
    }

    @Test
    void purge_nonEmptyCache_clearsAll() {
        db.add("user1");
        db.add("user2");

        cache.purge();
        assertTrue(db.isEmpty());
    }

    @Test
    void purge_emptyCache_noEffect() {
        cache.purge();
        assertTrue(db.isEmpty());
    }
}
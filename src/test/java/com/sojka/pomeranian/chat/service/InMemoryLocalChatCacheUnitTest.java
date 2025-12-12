package com.sojka.pomeranian.chat.service;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.model.ActiveUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLocalChatCacheUnitTest {

    Map<UUID, ActiveUser> db = new HashMap<>();
    ChatCacheInt cache = new InMemoryLocalChatCache(db);

    @BeforeEach
    void setUp() {
        db.clear();
    }

    UUID userId = UUID.randomUUID();

    @Test
    void put_newEntry_true() {
        db.put(userId, new ActiveUser(userId, new HashMap<>(), "", null));

        assertTrue(cache.put(userId, new StompSubscription(StompSubscription.Type.CHAT, userId + "")));
        assertTrue(db.containsKey(userId));
    }

    @Test
    void put_existingEntry_false() {
        db.put(userId, new ActiveUser(userId, Map.of("CHAT", new ArrayList<>(List.of(userId + ""))), null, null));

        assertFalse(cache.put(userId, new StompSubscription(StompSubscription.Type.CHAT, userId + "")));
        assertTrue(db.containsKey(userId));
    }

    @Test
    void isOnline_userPresent_true() {
        db.put(userId, new ActiveUser(userId, Map.of("CHAT", List.of(userId + "")), null, null));

        assertTrue(cache.isOnline(userId, new StompSubscription(StompSubscription.Type.CHAT, userId + "")));
    }

    @Test
    void isOnline_userAbsent_false() {
        assertFalse(cache.isOnline(userId, new StompSubscription(StompSubscription.Type.CHAT, userId + "")));
    }

    @Test
    void remove_existingUser_true() {
        db.put(userId, new ActiveUser(userId, Map.of("CHAT", List.of(userId + "")), null, null));

        assertTrue(cache.remove(userId));
        assertFalse(db.containsKey(userId));
    }

    @Test
    void remove_nonExistingUser_false() {
        assertFalse(cache.remove(userId));
        assertFalse(db.containsKey(userId));
    }

    @Test
    void purge_nonEmptyCache_clearsAll() {
        db.put(userId, new ActiveUser());
        db.put(UUID.randomUUID(), new ActiveUser());

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
        db.put(userId, new ActiveUser(userId, new HashMap<>(), "", null));

        assertFalse(cache.isOnline(userId, new StompSubscription(StompSubscription.Type.CHAT, "sub1")));
    }

    @Test
    void isOnline_userPresentNoMatchingSubscriptionId_false() {
        db.put(userId, new ActiveUser(userId, Map.of("CHAT", List.of("sub1")), "", null));

        assertFalse(cache.isOnline(userId, new StompSubscription(StompSubscription.Type.CHAT, "sub2")));
    }

    @Test
    void get_userPresent_returnsUser() {
        ActiveUser user = new ActiveUser(userId, Map.of("CHAT", List.of("sub1")), "session1", null);
        db.put(userId, user);

        assertTrue(cache.get(userId).isPresent());
        assertEquals(user, cache.get(userId).get());
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
        var userId2 = UUID.randomUUID();
        ActiveUser user1 = new ActiveUser(userId, Map.of("CHAT", List.of("sub1")), "session1", null);
        ActiveUser user2 = new ActiveUser(userId2, Map.of("NOTIFICATIONS", List.of("sub2")), "session2", null);
        db.put(userId, user1);
        db.put(userId2, user2);

        List<ActiveUser> result = cache.getAll();

        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of(user1, user2)));
    }

    @Test
    void put_userAbsent_false() {
        assertFalse(cache.put(userId, new StompSubscription(StompSubscription.Type.CHAT, "sub1")));
        assertFalse(db.containsKey(userId));
    }

    @Test
    void put_newSubscriptionType_true() {
        db.put(userId, new ActiveUser(userId, new HashMap<>(), "", null));
        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT_NOTIFICATIONS, "sub1");

        assertTrue(cache.put(userId, subscription));
        assertTrue(db.get(userId).getSubscriptions().containsKey("CHAT_NOTIFICATIONS"));
        assertTrue(db.get(userId).getSubscriptions().get("CHAT_NOTIFICATIONS").contains("sub1"));
    }

    @Test
    void create_newUser_true() {
        String sessionId = "session1";

        assertTrue(cache.create(userId, sessionId));
        assertTrue(db.containsKey(userId));
        assertEquals(userId, db.get(userId).getUserId());
        assertEquals(sessionId, db.get(userId).getSimpSessionId());
        assertTrue(db.get(userId).getSubscriptions().isEmpty());
    }

    @Test
    void create_existingUser_false() {
        db.put(userId, new ActiveUser(userId, new HashMap<>(), "session1", null));

        assertFalse(cache.create(userId, "session2"));
        assertEquals("session2", db.get(userId).getSimpSessionId());
    }

    @Test
    void removeSubscriptions_nullSubscriptionId_removesType() {
        Map<String, List<String>> subscriptions = new HashMap<>();
        subscriptions.put("CHAT", new ArrayList<>(List.of("sub1", "sub2")));
        db.put(userId, new ActiveUser(userId, subscriptions, "", null));
        List<StompSubscription> subs = List.of(new StompSubscription(StompSubscription.Type.CHAT, null));

        assertTrue(cache.remove(userId, subs));
        assertFalse(db.get(userId).getSubscriptions().containsKey("CHAT"));
    }

    @Test
    void removeSubscriptions_blankSubscriptionId_removesType() {
        Map<String, List<String>> subscriptions = new HashMap<>();
        subscriptions.put("CHAT", new ArrayList<>(List.of("sub1", "sub2")));
        db.put(userId, new ActiveUser(userId, subscriptions, "", null));
        List<StompSubscription> subs = List.of(new StompSubscription(StompSubscription.Type.CHAT, ""));

        assertTrue(cache.remove(userId, subs));
        assertFalse(db.get(userId).getSubscriptions().containsKey("CHAT"));
    }

    @Test
    void removeSubscriptions_validSubscriptionId_removesId() {
        Map<String, List<String>> subscriptions = new HashMap<>();
        subscriptions.put("CHAT", new ArrayList<>(List.of("sub1", "sub2")));
        db.put(userId, new ActiveUser(userId, subscriptions, "", null));
        List<StompSubscription> subs = List.of(new StompSubscription(StompSubscription.Type.CHAT, "sub1"));

        assertTrue(cache.remove(userId, subs));
        assertTrue(db.get(userId).getSubscriptions().containsKey("CHAT"));
        assertEquals(List.of("sub2"), db.get(userId).getSubscriptions().get("CHAT"));
    }

    @Test
    void removeSubscriptions_lastSubscriptionId_removesType() {
        Map<String, List<String>> subscriptions = new HashMap<>();
        subscriptions.put("CHAT", new ArrayList<>(List.of("sub1")));
        db.put(userId, new ActiveUser(userId, subscriptions, "", null));
        List<StompSubscription> subs = List.of(new StompSubscription(StompSubscription.Type.CHAT, "sub1"));

        assertTrue(cache.remove(userId, subs));
        assertFalse(db.get(userId).getSubscriptions().containsKey("CHAT"));
    }

    @Test
    void removeSubscriptions_userAbsent_false() {
        List<StompSubscription> subs = List.of(new StompSubscription(StompSubscription.Type.CHAT, "sub1"));

        assertFalse(cache.remove(userId, subs));
    }

    @Test
    void removeSubscriptions_emptySubscriptionsList_false() {
        db.put(userId, new ActiveUser(userId, new HashMap<>(), "", null));

        assertFalse(cache.remove(userId, Collections.emptyList()));
    }

    @Test
    void removeSubscriptions_nullSubscriptionsList_false() {
        db.put(userId, new ActiveUser(userId, new HashMap<>(), "", null));

        assertThatThrownBy(() -> cache.remove(userId, null))
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessage("subscriptions is marked non-null but is null");
    }

}
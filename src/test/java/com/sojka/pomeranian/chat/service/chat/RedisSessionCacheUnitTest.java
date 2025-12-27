package com.sojka.pomeranian.chat.service.chat;

import com.sojka.pomeranian.chat.dto.StompSubscription;
import com.sojka.pomeranian.chat.exception.CacheException;
import com.sojka.pomeranian.chat.model.ActiveUser;
import com.sojka.pomeranian.chat.service.cache.RedisSessionCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisSessionCacheUnitTest {

    private final ValueOperations<UUID, ActiveUser> usersOps = mock(ValueOperations.class);
    private final ValueOperations<String, UUID> sessionsOps = mock(ValueOperations.class);
    private final RedisTemplate<UUID, ActiveUser> users = mock(RedisTemplate.class);
    private final RedisTemplate<String, UUID> sessions = mock(RedisTemplate.class);

    private final RedisSessionCache cache = new RedisSessionCache(users, sessions);

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        doReturn(usersOps).when(users).opsForValue();
        doReturn(sessionsOps).when(sessions).opsForValue();
    }

    @Test
    void isOnline_userPresent_true() {
        ActiveUser activeUser = mock(ActiveUser.class);
        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, userId.toString());
        doReturn(activeUser).when(usersOps).get(userId);
        doReturn(true).when(activeUser).isOnline(subscription);

        assertTrue(cache.isOnline(userId, subscription));
    }

    @Test
    void isOnline_userAbsent_false() {
        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, userId.toString());
        doReturn(null).when(usersOps).get(userId);

        assertFalse(cache.isOnline(userId, subscription));
    }

    @Test
    void isOnlineType_userPresent_true() {
        ActiveUser activeUser = mock(ActiveUser.class);
        StompSubscription.Type type = StompSubscription.Type.CHAT;
        doReturn(activeUser).when(usersOps).get(userId);
        doReturn(true).when(activeUser).isOnline(type);

        assertTrue(cache.isOnline(userId, type));
    }

    @Test
    void isOnlineType_userAbsent_false() {
        StompSubscription.Type type = StompSubscription.Type.CHAT;
        doReturn(null).when(usersOps).get(userId);

        assertFalse(cache.isOnline(userId, type));
    }

    @Test
    void get_userPresent_returnsUser() {
        ActiveUser activeUser = new ActiveUser(userId, new ArrayList<>());
        doReturn(activeUser).when(usersOps).get(userId);

        Optional<ActiveUser> result = cache.get(userId);

        assertTrue(result.isPresent());
        assertEquals(activeUser, result.get());
    }

    @Test
    void get_userAbsent_returnsEmpty() {
        doReturn(null).when(usersOps).get(userId);

        Optional<ActiveUser> result = cache.get(userId);

        assertFalse(result.isPresent());
    }

    @Test
    void getAll_multipleUsers_returnsAllUsers() {
        UUID userId2 = UUID.randomUUID();
        ActiveUser user1 = new ActiveUser(userId, new ArrayList<>());
        ActiveUser user2 = new ActiveUser(userId2, new ArrayList<>());
        List<ActiveUser> users = List.of(user1, user2);

        doReturn(users).when(usersOps).multiGet(any());

        List<ActiveUser> result = cache.getAll();

        assertEquals(2, result.size());
        assertTrue(result.containsAll(users));
    }

    @Test
    void getAll_empty_returnsEmptyList() {
        List<ActiveUser> result = cache.getAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void add_newEntry_true() {
        String simpSessionId = "session1";
        ActiveUser.Session session = new ActiveUser.Session(new HashMap<>(), simpSessionId, Instant.now());
        ActiveUser activeUser = new ActiveUser(userId, new ArrayList<>(List.of(session)));
        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, "sub1");

        doReturn(activeUser).when(usersOps).get(userId);
        doReturn(true).when(usersOps).setIfPresent(eq(userId), any(ActiveUser.class));

        assertTrue(cache.add(userId, simpSessionId, subscription));
    }

    @Test
    void add_existingEntry_false() {
        String simpSessionId = "session1";
        Map<String, List<String>> subs = new HashMap<>();
        subs.put(StompSubscription.Type.CHAT.name(), new ArrayList<>(List.of("sub1")));
        ActiveUser.Session session = new ActiveUser.Session(subs, simpSessionId, Instant.now());
        ActiveUser activeUser = new ActiveUser(userId, new ArrayList<>(List.of(session)));
        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, "sub1");

        doReturn(activeUser).when(usersOps).get(userId);

        assertFalse(cache.add(userId, simpSessionId, subscription));
    }

    @Test
    void add_userAbsent_throws() {
        String simpSessionId = "session1";
        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, "sub1");
        doReturn(null).when(usersOps).get(userId);

        assertThatThrownBy(() -> cache.add(userId, simpSessionId, subscription))
                .isInstanceOf(CacheException.class)
                .hasMessage("User=%s is not online".formatted(userId));
    }

    @Test
    void add_nonExistingSession_throws() {
        String simpSessionId = "session1";
        String wrongSimpSessionId = "wrong";
        ActiveUser.Session session = new ActiveUser.Session(new HashMap<>(), simpSessionId, Instant.now());
        ActiveUser activeUser = new ActiveUser(userId, new ArrayList<>(List.of(session)));
        StompSubscription subscription = new StompSubscription(StompSubscription.Type.CHAT, "sub1");

        doReturn(activeUser).when(usersOps).get(userId);

        assertThatThrownBy(() -> cache.add(userId, wrongSimpSessionId, subscription))
                .isInstanceOf(CacheException.class)
                .hasMessage("User=%s do not have active simpSessionID=%s".formatted(userId, wrongSimpSessionId));
    }

    @Test
    void create_newUser_true() {
        String simpSessionId = "session1";
        doReturn(null).when(usersOps).get(userId);

        assertTrue(cache.create(userId, simpSessionId));
        verify(usersOps).set(eq(userId), any(ActiveUser.class));
        verify(sessionsOps).set(simpSessionId, userId);
    }

    @Test
    void create_existingUser_addsSession_true() {
        String simpSessionId1 = "session1";
        ActiveUser.Session session1 = new ActiveUser.Session(new HashMap<>(), simpSessionId1, Instant.now());
        ActiveUser activeUser = new ActiveUser(userId, new ArrayList<>(List.of(session1)));
        String simpSessionId2 = "session2";

        doReturn(activeUser).when(usersOps).get(userId);

        assertTrue(cache.create(userId, simpSessionId2));
        verify(usersOps).set(eq(userId), any(ActiveUser.class));
        verify(sessionsOps).set(simpSessionId2, userId);
    }

    @Test
    void create_duplicateSession_throws() {
        String simpSessionId = "session1";
        ActiveUser.Session session = new ActiveUser.Session(new HashMap<>(), simpSessionId, Instant.now());
        ActiveUser activeUser = new ActiveUser(userId, new ArrayList<>(List.of(session)));

        doReturn(activeUser).when(usersOps).get(userId);

        assertThatThrownBy(() -> cache.create(userId, simpSessionId))
                .isInstanceOf(CacheException.class)
                .hasMessage("User=%s, simpSessionId=%s already online".formatted(userId, simpSessionId));
    }

    @Test
    void remove_existingSession_returnsUserId() {
        String simpSessionId1 = "session1";
        String simpSessionId2 = "session2";
        ActiveUser.Session session1 = new ActiveUser.Session(new HashMap<>(), simpSessionId1, Instant.now());
        ActiveUser.Session session2 = new ActiveUser.Session(new HashMap<>(), simpSessionId2, Instant.now());
        ActiveUser activeUser = new ActiveUser(userId, new ArrayList<>(List.of(session1, session2)));

        doReturn(userId).when(sessionsOps).get(simpSessionId1);
        doReturn(activeUser).when(usersOps).get(userId);

        UUID result = cache.remove(simpSessionId1);

        assertEquals(userId, result);
        verify(sessions).delete(List.of(simpSessionId1, simpSessionId2));
        verify(users).delete(userId);
    }

    @Test
    void remove_nonExistingSession_throws() {
        String simpSessionId = "session1";
        doReturn(null).when(sessionsOps).get(simpSessionId);

        assertThatThrownBy(() -> cache.remove(simpSessionId))
                .isInstanceOf(CacheException.class)
                .hasMessage("Session=%s is not online".formatted(simpSessionId));
    }

    @Test
    void remove_userNotOnline_throws() {
        String simpSessionId = "session1";
        doReturn(userId).when(sessionsOps).get(simpSessionId);
        doReturn(null).when(usersOps).get(userId);

        assertThatThrownBy(() -> cache.remove(simpSessionId))
                .isInstanceOf(CacheException.class)
                .hasMessage("User=%s not online".formatted(userId));
    }

    @Test
    void removeSubscriptions_nullSubscriptionId_removesType() {
        String simpSessionId = "session1";
        Map<String, List<String>> subs = new HashMap<>();
        subs.put(StompSubscription.Type.CHAT.name(), new ArrayList<>(List.of("sub1", "sub2")));
        ActiveUser.Session session = new ActiveUser.Session(subs, simpSessionId, Instant.now());
        ActiveUser activeUser = new ActiveUser(userId, new ArrayList<>(List.of(session)));
        List<StompSubscription> subscriptions = List.of(new StompSubscription(StompSubscription.Type.CHAT, null));

        doReturn(activeUser).when(usersOps).get(userId);

        assertTrue(cache.remove(userId, simpSessionId, subscriptions));

        ArgumentCaptor<ActiveUser> captor = ArgumentCaptor.forClass(ActiveUser.class);
        verify(usersOps).set(eq(userId), captor.capture());
        ActiveUser updated = captor.getValue();
        assertFalse(updated.getSessions().get(0).getSubscriptions().containsKey(StompSubscription.Type.CHAT.name()));
    }

    @Test
    void removeSubscriptions_blankSubscriptionId_removesType() {
        String simpSessionId = "session1";
        Map<String, List<String>> subs = new HashMap<>();
        subs.put(StompSubscription.Type.CHAT.name(), new ArrayList<>(List.of("sub1", "sub2")));
        ActiveUser.Session session = new ActiveUser.Session(subs, simpSessionId, Instant.now());
        ActiveUser activeUser = new ActiveUser(userId, new ArrayList<>(List.of(session)));
        List<StompSubscription> subscriptions = List.of(new StompSubscription(StompSubscription.Type.CHAT, ""));

        doReturn(activeUser).when(usersOps).get(userId);

        assertTrue(cache.remove(userId, simpSessionId, subscriptions));

        ArgumentCaptor<ActiveUser> captor = ArgumentCaptor.forClass(ActiveUser.class);
        verify(usersOps).set(eq(userId), captor.capture());
        ActiveUser updated = captor.getValue();
        assertFalse(updated.getSessions().get(0).getSubscriptions().containsKey(StompSubscription.Type.CHAT.name()));
    }

    @Test
    void removeSubscriptions_validSubscriptionId_removesId() {
        String simpSessionId = "session1";
        Map<String, List<String>> subs = new HashMap<>();
        subs.put(StompSubscription.Type.CHAT.name(), new ArrayList<>(List.of("sub1", "sub2")));
        ActiveUser.Session session = new ActiveUser.Session(subs, simpSessionId, Instant.now());
        ActiveUser activeUser = new ActiveUser(userId, new ArrayList<>(List.of(session)));
        List<StompSubscription> subscriptions = List.of(new StompSubscription(StompSubscription.Type.CHAT, "sub1"));

        doReturn(activeUser).when(usersOps).get(userId);

        assertTrue(cache.remove(userId, simpSessionId, subscriptions));

        ArgumentCaptor<ActiveUser> captor = ArgumentCaptor.forClass(ActiveUser.class);
        verify(usersOps).set(eq(userId), captor.capture());
        ActiveUser updated = captor.getValue();
        assertTrue(updated.getSessions().get(0).getSubscriptions().containsKey(StompSubscription.Type.CHAT.name()));
        assertEquals(List.of("sub2"), updated.getSessions().get(0).getSubscriptions().get(StompSubscription.Type.CHAT.name()));
    }

    @Test
    void removeSubscriptions_lastSubscriptionId_removesType() {
        String simpSessionId = "session1";
        Map<String, List<String>> subs = new HashMap<>();
        subs.put(StompSubscription.Type.CHAT.name(), new ArrayList<>(List.of("sub1")));
        ActiveUser.Session session = new ActiveUser.Session(subs, simpSessionId, Instant.now());
        ActiveUser activeUser = new ActiveUser(userId, new ArrayList<>(List.of(session)));
        List<StompSubscription> subscriptions = List.of(new StompSubscription(StompSubscription.Type.CHAT, "sub1"));

        doReturn(activeUser).when(usersOps).get(userId);

        assertTrue(cache.remove(userId, simpSessionId, subscriptions));

        ArgumentCaptor<ActiveUser> captor = ArgumentCaptor.forClass(ActiveUser.class);
        verify(usersOps).set(eq(userId), captor.capture());
        ActiveUser updated = captor.getValue();
        assertFalse(updated.getSessions().get(0).getSubscriptions().containsKey(StompSubscription.Type.CHAT.name()));
    }

    @Test
    void removeSubscriptions_userAbsent_throws() {
        String simpSessionId = "session1";
        List<StompSubscription> subscriptions = List.of(new StompSubscription(StompSubscription.Type.CHAT, "sub1"));
        doReturn(null).when(usersOps).get(userId);

        assertThatThrownBy(() -> cache.remove(userId, simpSessionId, subscriptions))
                .isInstanceOf(CacheException.class)
                .hasMessage("User=%s is not online".formatted(userId));
    }

}
package com.sojka.pomeranian.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisWebSocketServiceUnitTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ObjectMapper mapper = mock(ObjectMapper.class);

    private final RedisWebSocketService service = new RedisWebSocketService(redisTemplate, mapper);

    @BeforeEach
    void setUp() throws Exception {
        when(mapper.writeValueAsString(any())).thenReturn("{\"destination\":\"/queue/test\",\"payload\":\"hello\"}");
    }

    @Test
    void convertAndSend_happyPath_publishesToRedis() throws Exception {
        service.convertAndSend("/queue/test", "hello");

        verify(mapper).writeValueAsString(any(WebSocketMessage.class));
        verify(redisTemplate).convertAndSend(eq("websocket-channel"), eq("{\"destination\":\"/queue/test\",\"payload\":\"hello\"}"));
    }

    @Test
    void convertAndSendToUser_happyPath_expandsDestinationAndPublishes() throws Exception {
        service.convertAndSendToUser("user-123", "/queue/private", "data");

        verify(mapper).writeValueAsString(any(WebSocketMessage.class));
        verify(redisTemplate).convertAndSend(eq("websocket-channel"), any(String.class));
    }

    @Test
    void convertAndSend_serializationFailure_throwsMessagingException() throws Exception {
        when(mapper.writeValueAsString(any())).thenThrow(new RuntimeException("bad json"));

        assertThatThrownBy(() -> service.convertAndSend("/queue/fail", "payload"))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("Failed to publish WebSocket message via Redis")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void convertAndSend_redisFailure_throwsMessagingException() throws Exception {
        doThrow(new RuntimeException("redis down")).when(redisTemplate).convertAndSend(any(), any());

        assertThatThrownBy(() -> service.convertAndSend("/queue/fail", "payload"))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("Failed to publish WebSocket message via Redis");
    }

    @Test
    void convertAndSendToUser_withHeaders_throwsImmediately() {
        // These overloads must throw so it is immediately obvious that Redis bridge is not being used
        assertThatThrownBy(() ->
                service.convertAndSendToUser("user-1", "/q/test", "payload", (java.util.Map<String, Object>) null)
        ).isInstanceOf(MessagingException.class);
    }

    @Test
    void send_methods_throwMessagingException() {
        assertThatThrownBy(() -> service.send((org.springframework.messaging.Message<?>) null))
                .isInstanceOf(MessagingException.class);
    }
}
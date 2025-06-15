package com.sojka.pomeranian.chat.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatResponseUnitTest {

    @Test
    void chatResponse_chatMessagePersisted_created() {
        var chatResponse = new ChatResponse<>(new ChatMessagePersisted());
        assertThat(chatResponse.getType()).isEqualTo(MessageType.CHAT);
    }

    @Test
    void chatResponse_unsupportedClass_runtimeException() {
        assertThatThrownBy(() -> new ChatResponse<>(Instant.now()))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Unrecognized chat response type: class java.time.Instant");
    }

    @Test
    void chatResponse_emptyList_illegalArgumentException() {
        assertThatThrownBy(() -> new ChatResponse<>(Collections.emptyList()))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Response list cannot be empty");
    }
}
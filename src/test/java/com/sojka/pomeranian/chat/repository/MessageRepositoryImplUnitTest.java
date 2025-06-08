package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.sojka.pomeranian.chat.db.AstraConnector;
import com.sojka.pomeranian.chat.exception.AstraException;
import com.sojka.pomeranian.chat.model.Message;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageRepositoryImplUnitTest {

    CqlSession mockedSection = mock(CqlSession.class);
    MessageRepository repository = new MessageRepositoryImpl(new AstraDummyConnector(mockedSection));

    @Test
    void findByRoomId_invalidPageState_throwIllegalArgumentException() {
        assertThatThrownBy(() -> repository.findByRoomId("dummyRoomId", "ERROR", 10))
                .isExactlyInstanceOf(AstraException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid pageState for room_id dummyRoomId: Last unit does not have enough valid bits");
    }

    @Test
    void findByRoomId_sessionNotInitialized_throwIllegalStateException() {
        when(mockedSection.execute(any(SimpleStatement.class))).thenThrow(new IllegalStateException("session error"));
        assertThatThrownBy(() -> repository.findByRoomId("dummyRoomId", null, 10))
                .isExactlyInstanceOf(AstraException.class)
                .hasCauseExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cassandra session not initialized");
    }

    @Test
    void findByRoomId_unexpectedException_throwRuntimeException() {
        when(mockedSection.execute(any(SimpleStatement.class))).thenThrow(new RuntimeException("unexpected error"));
        assertThatThrownBy(() -> repository.findByRoomId("dummyRoomId", null, 10))
                .isExactlyInstanceOf(AstraException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Unexpected issue");
    }

    @Test
    void save_unexpectedException_throwRuntimeException() {
        when(mockedSection.execute(any(SimpleStatement.class))).thenThrow(new RuntimeException("unexpected error"));
        assertThatThrownBy(() -> repository.save(Message.builder().roomId("user1:user2").content("dummy").username("dummy").build()))
                .isExactlyInstanceOf(AstraException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Failed to save message for room_id=user1:user2");
    }

    public static class AstraDummyConnector extends AstraConnector {

        private final CqlSession cqlSession;

        public AstraDummyConnector(CqlSession cqlSession) {
            super(null);
            this.cqlSession = cqlSession;
        }

        @Override
        public CqlSession connect() {
            return cqlSession;
        }

        @Override
        public CqlSession getSession() {
            return cqlSession;
        }
    }

}
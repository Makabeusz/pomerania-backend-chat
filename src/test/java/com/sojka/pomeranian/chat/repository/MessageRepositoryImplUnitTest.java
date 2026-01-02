package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.sojka.pomeranian.astra.connection.Connector;
import com.sojka.pomeranian.astra.exception.AstraException;
import com.sojka.pomeranian.chat.model.Message;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageRepositoryImplUnitTest {

    CqlSession mockedSection = mock(CqlSession.class);
    MessageRepository repository = new MessageRepository(new AstraDummyConnector(mockedSection));

    @Test
    void findByRoomId_invalidPageState_throwIllegalArgumentException() {
        assertThatThrownBy(() -> repository.findByRoomId("dummyRoomId", "ERROR", 10))
                .isExactlyInstanceOf(AstraException.class)
                .hasMessage("Failed to execute findByRoomId, for: RoomIdState[roomId=dummyRoomId, pageState=ERROR, pageSize=10]")
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Invalid pageState: Last unit does not have enough valid bits");
    }

    @Test
    void findByRoomId_sessionNotInitialized_throwIllegalStateException() {
        when(mockedSection.execute(any(SimpleStatement.class))).thenThrow(new IllegalStateException("session error"));
        assertThatThrownBy(() -> repository.findByRoomId("dummyRoomId", null, 10))
                .isExactlyInstanceOf(AstraException.class)
                .hasMessage("Cassandra session not initialized, id=RoomIdState[roomId=dummyRoomId, pageState=null, pageSize=10]")
                .hasCauseExactlyInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("session error");
    }

    @Test
    void findByRoomId_unexpectedException_throwRuntimeException() {
        when(mockedSection.execute(any(SimpleStatement.class))).thenThrow(new RuntimeException("unexpected error"));
        assertThatThrownBy(() -> repository.findByRoomId("dummyRoomId", null, 10))
                .isExactlyInstanceOf(AstraException.class)
                .hasMessage("Failed to execute findByRoomId, for: RoomIdState[roomId=dummyRoomId, pageState=null, pageSize=10]")
                .hasCauseExactlyInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("unexpected error");
    }

    @Test
    void save_unexpectedException_throwRuntimeException() {
        when(mockedSection.execute(any(SimpleStatement.class))).thenThrow(new RuntimeException("unexpected error"));
        assertThatThrownBy(() -> repository.save(Message.builder().roomId("user1:user2").content("dummy").username("dummy").build()))
                .isExactlyInstanceOf(AstraException.class)
                .hasMessage("Failed to execute save, for: Message(roomId=user1:user2, createdAt=null, profileId=null, username=dummy, recipientProfileId=null, recipientUsername=null, content=dummy, resourceId=null, resourceType=null, editedAt=null, readAt=null, metadata=null)")
                .hasCauseExactlyInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("unexpected error");
    }

    public static class AstraDummyConnector extends Connector {

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
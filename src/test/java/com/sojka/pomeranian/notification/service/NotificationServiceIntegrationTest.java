package com.sojka.pomeranian.notification.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.sojka.pomeranian.TestcontainersConfiguration;
import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.db.AstraTestcontainersConnector;
import com.sojka.pomeranian.chat.util.TestUtils;
import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.lib.dto.UserData;
import com.sojka.pomeranian.lib.util.JsonUtils;
import com.sojka.pomeranian.notification.model.NotificationModel;
import com.sojka.pomeranian.notification.model.ReadNotification;
import com.sojka.pomeranian.notification.repository.NotificationRepository;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.sojka.pomeranian.lib.dto.NotificationType.FOLLOW;
import static com.sojka.pomeranian.lib.util.CommonUtils.getNameOrNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import({TestcontainersConfiguration.class})
@SpringBootTest
class NotificationServiceIntegrationTest {

    @Autowired
    NotificationService notificationService;
    @Autowired
    NotificationRepository<NotificationModel> notificationRepository;
    @Autowired
    NotificationRepository<ReadNotification> readNotificationRepository;
    @Autowired
    AstraTestcontainersConnector connector;

    CqlSession session;

    @BeforeEach
    void setUp() {
        session = connector.connect();
        session.execute("TRUNCATE notifications.notifications");
        session.execute("TRUNCATE notifications.read_notifications");
    }

    UUID user1 = UUID.randomUUID();
    UUID user2 = UUID.randomUUID();

    @Test
    void publish_notification_savedAndSentIfOnline() {
        Notification<Object> notification = Notification.builder()
                .profileId(user1)
                .sender(UserData.builder().id(UUID.randomUUID()).build())
                .type(FOLLOW)
                .build();

        var createdAt = notificationService.process(notification);

        // Verify notification in notifications table
        NotificationModel saved = TestUtils.getNotification(connector, notification.getProfileId(),
                createdAt, getNameOrNull(notification.getType())
        );
        assertThat(saved).usingRecursiveComparison(new RecursiveComparisonConfiguration())
                .isEqualTo(NotificationModel.builder()
                        .profileId(user1)
                        .type(FOLLOW)
                        .createdAt(createdAt)
                        .build());

        assertThat(notification.getType()).isEqualTo(FOLLOW);
    }

//    TODO: test ok, but have JSON mapping issues and basically test not existing scenario (body not present in prod)
//    @Test
//    void markRead_validNotifications_readAtUpdatedAndMovedToRead() {
//        var body = JsonUtils.writeToString(Map.of("content", "Followed!"));
//        NotificationModel notification = NotificationModel.builder()
//                .profileId(user1)
//                .type(FOLLOW)
//                .body(body)
//                .createdAt(Instant.now())
//                .build();
//        notificationRepository.save(notification);
//        Notification dto = NotificationMapper.toDto(notification);
//        List<Notification> notifications = List.of(dto);
//
//        Instant readAt = notificationService.markRead(user1, notifications);
//
//        // Verify notification deleted from notifications
//        Optional<NotificationModel> notExisting = notificationRepository.findById(notification.getProfileId(),
//                notification.getCreatedAt(), notification.getType());
//        assertThat(notExisting).isEmpty();
//
//        // Verify moved to read_notifications
//        ReadNotification readNotification = TestUtils.getReadNotification(
//                connector, notification.getProfileId(), notification.getCreatedAt(), notification.getType().name()
//        );
//        assertThat(readNotification).usingRecursiveComparison(new RecursiveComparisonConfiguration())
//                .ignoringFields("createdAt", "readAt")
//                .isEqualTo(ReadNotification.builder()
//                        .profileId(user1)
//                        .type(FOLLOW)
//                        .body(body)
//                        .build());
//        assertThat(readNotification.getReadAt()).isEqualTo(readAt);
//    }

    @Test
    void getUnread_fewNotifications_sortedByCreatedAtDesc() {
        NotificationModel notification1 = NotificationModel.builder()
                .profileId(user1)
                .type(FOLLOW)
                .body(JsonUtils.writeToString(Map.of("content", "Old follow")))
                .createdAt(Instant.now().minusSeconds(10))
                .build();
        NotificationModel notification2 = NotificationModel.builder()
                .profileId(user1)
                .type(FOLLOW)
                .body(JsonUtils.writeToString(Map.of("content", "New follow")))
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        ResultsPage<Notification<Object>> response = notificationService.getUnread(user1, null, 10);

        assertEquals(2, response.getResults().size());
        assertEquals("New follow", ((Map) response.getResults().get(0).getBody()).get("content"));
        assertEquals("Old follow", ((Map) response.getResults().get(1).getBody()).get("content"));
        assertNull(response.getNextPageState());
    }

    @ParameterizedTest
    @MethodSource("paginationSource")
    void getUnread_manyNotifications_paginatedResults(boolean fetchNextPage) {
        for (int i = 1; i <= 15; i++) {
            NotificationModel notification = NotificationModel.builder()
                    .profileId(user1)
                    .type(FOLLOW)
                    .body(JsonUtils.writeToString(Map.of("content", "Notification " + i)))
                    .createdAt(Instant.now().minusSeconds(15 - i))
                    .build();
            notificationRepository.save(notification);
        }

        ResultsPage<Notification<Object>> response = notificationService.getUnread(user1, null, 10);

        if (fetchNextPage) {
            assertEquals(10, response.getResults().size());
            assertEquals("Notification 15", ((Map) response.getResults().getFirst().getBody()).get("content"));
            assertNotNull(response.getNextPageState());
        } else {
            response = notificationService.getUnread(user1, response.getNextPageState(), 10);
            assertEquals(5, response.getResults().size());
            assertEquals("Notification 5", ((Map) response.getResults().getFirst().getBody()).get("content"));
            assertNull(response.getNextPageState());
        }
    }

    static Stream<Arguments> paginationSource() {
        return Stream.of(
                Arguments.of(false),
                Arguments.of(true)
        );
    }

    @Test
    void countUnreadNotifications_multipleNotifications_correctCount() {
        Instant now = Instant.now();
        NotificationModel notification1 = NotificationModel.builder()
                .profileId(user1)
                .type(FOLLOW)
                .body(JsonUtils.writeToString(Map.of("content", "You have been followed by X")))
                .createdAt(now)
                .build();
        NotificationModel notification2 = NotificationModel.builder()
                .profileId(user1)
                .type(FOLLOW)
                .body(JsonUtils.writeToString(Map.of("content", "You have been followed by Y")))
                .createdAt(now.plusMillis(1L))
                .build();
        NotificationModel otherUserNotification = NotificationModel.builder()
                .profileId(user2)
                .type(FOLLOW)
                .body(JsonUtils.writeToString(Map.of("content", "Message 3")))
                .createdAt(now)
                .build();
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(otherUserNotification);

        Long count = notificationService.countUnreadNotifications(user1);

        assertEquals(2L, count);
    }

    @Test
    void getRead_fewReadNotifications_sortedByCreatedAtDesc() {
        ReadNotification read1 = ReadNotification.builder()
                .profileId(user1)
                .type(FOLLOW)
                .body(JsonUtils.writeToString(Map.of("content", "Old read")))
                .createdAt(Instant.now().minusSeconds(10))
                .readAt(Instant.now())
                .build();
        ReadNotification read2 = ReadNotification.builder()
                .profileId(user1)
                .type(FOLLOW)
                .body(JsonUtils.writeToString(Map.of("content", "New read")))
                .createdAt(Instant.now())
                .readAt(Instant.now())
                .build();
        readNotificationRepository.save(read1);
        readNotificationRepository.save(read2);

        ResultsPage<Notification<Object>> response = notificationService.getRead(user1, null, 10);

        assertEquals(2, response.getResults().size());
        assertEquals("New read", ((Map) response.getResults().get(0).getBody()).get("content"));
        assertEquals("Old read", ((Map) response.getResults().get(1).getBody()).get("content"));
        assertNull(response.getNextPageState());
    }
}
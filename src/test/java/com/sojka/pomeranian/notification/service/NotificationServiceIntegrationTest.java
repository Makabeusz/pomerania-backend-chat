package com.sojka.pomeranian.notification.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.sojka.pomeranian.TestcontainersConfiguration;
import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.db.AstraTestcontainersConnector;
import com.sojka.pomeranian.chat.dto.NotificationResponse;
import com.sojka.pomeranian.chat.util.TestUtils;
import com.sojka.pomeranian.lib.dto.NotificationDto;
import com.sojka.pomeranian.lib.util.DateTimeUtils;
import com.sojka.pomeranian.notification.model.Notification;
import com.sojka.pomeranian.notification.model.ReadNotification;
import com.sojka.pomeranian.notification.repository.NotificationRepository;
import com.sojka.pomeranian.notification.repository.ReadNotificationRepository;
import com.sojka.pomeranian.notification.util.NotificationMapper;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.sojka.pomeranian.lib.dto.NotificationDto.Type.FOLLOW;
import static com.sojka.pomeranian.lib.util.CommonUtils.getNameOrNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import({TestcontainersConfiguration.class})
@SpringBootTest
class NotificationServiceIntegrationTest {

    @Autowired
    NotificationService notificationService;
    @Autowired
    NotificationRepository notificationRepository;
    @Autowired
    ReadNotificationRepository readNotificationRepository;
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
        NotificationDto notification = NotificationDto.builder()
                .profileId(user1)
                .type(FOLLOW)
                .content("New follow!")
                .build();

        NotificationResponse<NotificationDto> response = notificationService.publish(notification);

        // Verify notification in notifications table
        Notification saved = TestUtils.getNotification(connector, notification.getProfileId(),
                DateTimeUtils.toInstant(response.getData().getCreatedAt()), getNameOrNull(notification.getType())
        );
        assertThat(saved).usingRecursiveComparison(new RecursiveComparisonConfiguration())
                .ignoringFields("createdAt")
                .isEqualTo(Notification.builder()
                        .profileId(user1)
                        .type(FOLLOW)
                        .content("New follow!")
                        .build());

        assertThat(saved).usingRecursiveComparison(new RecursiveComparisonConfiguration())
                .ignoringFields("createdAt", "type")
                .isEqualTo(notification);
        assertThat(response.getType()).isEqualTo(FOLLOW);
    }

    @Test
    void markRead_validNotifications_readAtUpdatedAndMovedToRead() {
        Notification notification = Notification.builder()
                .profileId(user1)
                .type(FOLLOW)
                .content("Followed!")
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(notification);
        NotificationDto dto = NotificationMapper.toDto(notification);
        List<NotificationDto> notifications = List.of(dto);

        Instant readAt = notificationService.markRead(user1, notifications);

        // Verify notification deleted from notifications
        Optional<Notification> notExisting = notificationRepository.findById(notification.getProfileId(),
                notification.getCreatedAt(), notification.getType());
        assertThat(notExisting).isEmpty();

        // Verify moved to read_notifications
        ReadNotification readNotification = TestUtils.getReadNotification(
                connector, notification.getProfileId(), notification.getCreatedAt(), notification.getType().name()
        );
        assertThat(readNotification).usingRecursiveComparison(new RecursiveComparisonConfiguration())
                .ignoringFields("createdAt", "readAt")
                .isEqualTo(ReadNotification.builder()
                        .profileId(user1)
                        .type(FOLLOW)
                        .content("Followed!")
                        .build());
        assertThat(readNotification.getReadAt()).isEqualTo(readAt);
    }

    @Test
    void markRead_invalidUser_throwsSecurityException() {
        NotificationDto otherUserNotification = NotificationDto.builder()
                .profileId(user2)
                .type(FOLLOW)
                .content("Followed!")
                .build();
        List<NotificationDto> notifications = List.of(otherUserNotification);

        assertThrows(SecurityException.class, () -> notificationService.markRead(user1, notifications));
    }

    @Test
    void getUnread_fewNotifications_sortedByCreatedAtDesc() {
        Notification notification1 = Notification.builder()
                .profileId(user1)
                .type(FOLLOW)
                .content("Old follow")
                .createdAt(Instant.now().minusSeconds(10))
                .build();
        Notification notification2 = Notification.builder()
                .profileId(user1)
                .type(FOLLOW)
                .content("New follow")
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        ResultsPage<NotificationDto> response = notificationService.getUnread(user1, null, 10);

        assertEquals(2, response.getResults().size());
        assertEquals("New follow", response.getResults().get(0).getContent());
        assertEquals("Old follow", response.getResults().get(1).getContent());
        assertNull(response.getNextPageState());
    }

    @ParameterizedTest
    @MethodSource("paginationSource")
    void getUnread_manyNotifications_paginatedResults(boolean fetchNextPage) {
        for (int i = 1; i <= 15; i++) {
            Notification notification = Notification.builder()
                    .profileId(user1)
                    .type(FOLLOW)
                    .content("Notification " + i)
                    .createdAt(Instant.now().minusSeconds(15 - i))
                    .build();
            notificationRepository.save(notification);
        }

        ResultsPage<NotificationDto> response = notificationService.getUnread(user1, null, 10);

        if (fetchNextPage) {
            assertEquals(10, response.getResults().size());
            assertEquals("Notification 15", response.getResults().getFirst().getContent());
            assertNotNull(response.getNextPageState());
        } else {
            response = notificationService.getUnread(user1, response.getNextPageState(), 10);
            assertEquals(5, response.getResults().size());
            assertEquals("Notification 5", response.getResults().getFirst().getContent());
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
        Notification notification1 = Notification.builder()
                .profileId(user1)
                .type(FOLLOW)
                .content("You have been followed by X")
                .createdAt(now)
                .build();
        Notification notification2 = Notification.builder()
                .profileId(user1)
                .type(FOLLOW)
                .content("You have been followed by Y")
                .createdAt(now.plusMillis(1L))
                .build();
        Notification otherUserNotification = Notification.builder()
                .profileId(user2)
                .type(FOLLOW)
                .content("Message 3")
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
                .content("Old read")
                .createdAt(Instant.now().minusSeconds(10))
                .readAt(Instant.now())
                .build();
        ReadNotification read2 = ReadNotification.builder()
                .profileId(user1)
                .type(FOLLOW)
                .content("New read")
                .createdAt(Instant.now())
                .readAt(Instant.now())
                .build();
        readNotificationRepository.save(read1, 2137);
        readNotificationRepository.save(read2, 2137);

        ResultsPage<NotificationDto> response = notificationService.getRead(user1, null, 10);

        assertEquals(2, response.getResults().size());
        assertEquals("New read", response.getResults().get(0).getContent());
        assertEquals("Old read", response.getResults().get(1).getContent());
        assertNull(response.getNextPageState());
    }
}
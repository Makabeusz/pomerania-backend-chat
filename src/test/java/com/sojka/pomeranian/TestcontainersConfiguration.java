package com.sojka.pomeranian;

import com.sojka.pomeranian.pubsub.CommentsSubscriber;
import com.sojka.pomeranian.pubsub.DeleteAccountSubscriber;
import com.sojka.pomeranian.pubsub.NotificationSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.Mockito.mock;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    Logger log = LoggerFactory.getLogger(TestcontainersConfiguration.class);

    @Bean
    @SuppressWarnings("resource") // Spring container manages the resource lifecycle, no need for try-with-resources
    public CassandraContainer<?> cassandraContainer() {
        return new CassandraContainer<>(DockerImageName.parse("datastax/dse-server:6.8.50").asCompatibleSubstituteFor("cassandra"))
                .withEnv("DS_LICENSE", "accept")
                .withExposedPorts(9042)
                .withInitScript("init.cql")
                .withStartupTimeout(java.time.Duration.ofSeconds(300))
//                .withLogConsumer(new Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger("astra-container")))
                ;
    }

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource") // Spring container manages the resource lifecycle, no need for try-with-resources
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:17-3.5")
                .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("mydb")
                .withUsername("myuser")
                .withPassword("password")
                .withInitScript("init.sql");
    }

    @Bean
    @Primary
    NotificationSubscriber notificationSubscriber() {
        return mock(NotificationSubscriber.class);
    }

    @Bean
    @Primary
    CommentsSubscriber commentsSubscriber() {
        return mock(CommentsSubscriber.class);
    }

    @Bean
    @Primary
    DeleteAccountSubscriber deleteAccountSubscriber() {
        return mock(DeleteAccountSubscriber.class);
    }

}

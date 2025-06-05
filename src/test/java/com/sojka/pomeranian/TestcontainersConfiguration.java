package com.sojka.pomeranian;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    Logger log = LoggerFactory.getLogger(TestcontainersConfiguration.class);

    @Bean
    @ServiceConnection
    CassandraContainer<?> cassandraContainer() {
        return new CassandraContainer<>(DockerImageName.parse("cassandra:4.1.3"))
                .withInitScript("init.cql")
                .withLogConsumer(f -> log.info(f.getUtf8String()))
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                        .withMemory(8_000_000_000L)
                        .withCpuCount(4L)
                );
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("mydb")
                .withUsername("myuser")
                .withPassword("password")
                .withInitScript("init.sql");
    }

}

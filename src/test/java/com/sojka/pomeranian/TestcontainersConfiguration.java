package com.sojka.pomeranian;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    CassandraContainer<?> cassandraContainer() throws IOException {
        return new CassandraContainer<>(DockerImageName.parse("cassandra:4.1.3"))
                .withInitScript("init.cql");
    }

}

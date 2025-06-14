package com.sojka.pomeranian;

import com.sojka.pomeranian.astra.connection.Connector;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@RequiredArgsConstructor
@EnableJpaRepositories(
        basePackages = {"com.sojka.pomeranian.chat.repository"}
)
public class PomeranianChatApplication {

    private final Connector connector;

    public static void main(String[] args) {
        SpringApplication.run(PomeranianChatApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        connector.initialize();
    }
}

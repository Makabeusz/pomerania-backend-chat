package com.sojka.pomeranian;

import com.sojka.pomeranian.chat.db.AstraConnector;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@RequiredArgsConstructor
@EnableJpaRepositories( // check if security works
        basePackages = {"com.sojka.pomeranian.chat.repository"}
)
public class PomeranianChatApplication {

    private final AstraConnector connector;

    public static void main(String[] args) {
        SpringApplication.run(PomeranianChatApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        connector.initialize();
    }
}

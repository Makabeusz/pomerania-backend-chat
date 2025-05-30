package com.sojka.pomeranian;

import com.sojka.pomeranian.chat.db.AstraConnector;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@RequiredArgsConstructor
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

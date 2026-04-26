package com.sojka.pomeranian;

import com.sojka.pomeranian.pubsub.BlockUserSubscriber;
import com.sojka.pomeranian.pubsub.CommentsSubscriber;
import com.sojka.pomeranian.pubsub.DeleteAccountSubscriber;
import com.sojka.pomeranian.pubsub.NotificationSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@EnableCaching
@SpringBootApplication
@RequiredArgsConstructor
@EnableJpaRepositories(
        basePackages = {"com.sojka.pomeranian.chat.repository"}
)
public class PomeranianChatApplication {

    private final NotificationSubscriber notificationSubscriber;
    private final CommentsSubscriber commentsSubscriber;
    private final DeleteAccountSubscriber deleteAccountSubscriber;
    private final BlockUserSubscriber blockUserSubscriber;

    public static void main(String[] args) {
        SpringApplication.run(PomeranianChatApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Starting Pub/Sub subscribers");
        notificationSubscriber.subscribeAsync();
        commentsSubscriber.subscribeAsync();
        deleteAccountSubscriber.subscribeAsync();
        blockUserSubscriber.subscribeAsync();
        log.info("✅ Application is ready");
    }
}

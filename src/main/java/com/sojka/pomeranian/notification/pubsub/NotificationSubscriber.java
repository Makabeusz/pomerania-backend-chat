package com.sojka.pomeranian.notification.pubsub;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.sojka.pomeranian.chat.dto.NotificationDto;
import com.sojka.pomeranian.chat.util.JsonUtils;
import com.sojka.pomeranian.notification.pubsub.config.GcpConfig;
import com.sojka.pomeranian.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSubscriber {

    private final GcpConfig gcpConfig;
    private final NotificationService notificationService;

    Subscriber subscriber;

    public Subscriber subscribeAsync() {
        ProjectSubscriptionName subscriptionName =
                ProjectSubscriptionName.of(gcpConfig.getProjectId(), gcpConfig.getSubscriberConfig().getSubscriptionName());

        MessageReceiver receiver =
                (PubsubMessage message, AckReplyConsumer consumer) -> {
                    var notification = JsonUtils.readObject(message.getData().toByteArray(), NotificationDto.class);

                    notificationService.publish(notification);
                    consumer.ack();
                };

        subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();

        // Start the subscriber.
        subscriber.startAsync().awaitRunning();
        log.info("Listening for messages on {}", subscriptionName);

        return subscriber;
    }
}

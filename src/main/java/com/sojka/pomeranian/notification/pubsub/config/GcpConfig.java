package com.sojka.pomeranian.notification.pubsub.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
public class GcpConfig {

    String projectId;
    String region;
    SubscriberConfig subscriberConfig;

    public GcpConfig(
            @Value("${gcp.project-id}") String projectId,
            @Value("${gcp.region}") String region,
            SubscriberConfig subscriberConfig) {
        this.projectId = projectId;
        this.region = region;
        this.subscriberConfig = subscriberConfig;
    }

    @Data
    @Builder
    @Component
    @NoArgsConstructor
    @AllArgsConstructor
    @ConfigurationProperties("gcp.pubsub.subscriber")
    public static class SubscriberConfig {

        String subscriptionName;
    }

}

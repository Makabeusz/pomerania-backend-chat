package com.sojka.pomeranian.pubsub.config;

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
    NotificationsConfig notificationsConfig;
    CommentsConfig commentsConfig;
    DeleteAccountConfig deleteAccountConfig;
    ChatResourcesConfig chatResourcesConfig;

    public GcpConfig(
            @Value("${gcp.project-id}") String projectId,
            @Value("${gcp.region}") String region,
            NotificationsConfig notificationsConfig,
            CommentsConfig commentsConfig,
            DeleteAccountConfig deleteAccountConfig,
            ChatResourcesConfig chatResourcesConfig) {
        this.projectId = projectId;
        this.region = region;
        this.notificationsConfig = notificationsConfig;
        this.commentsConfig = commentsConfig;
        this.deleteAccountConfig = deleteAccountConfig;
        this.chatResourcesConfig = chatResourcesConfig;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Component
    @ConfigurationProperties("gcp.pubsub.chat-resources.publisher")
    public static class ChatResourcesConfig {

        String topicName;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Component
    @ConfigurationProperties("gcp.pubsub.notifications.subscriber")
    public static class NotificationsConfig {

        String subscriptionName;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Component
    @ConfigurationProperties("gcp.pubsub.comments.subscriber")
    public static class CommentsConfig {

        String subscriptionName;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Component
    @ConfigurationProperties("gcp.pubsub.delete-account.subscriber")
    public static class DeleteAccountConfig {

        String subscriptionName;
    }

}

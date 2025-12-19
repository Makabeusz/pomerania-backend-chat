package com.sojka.pomeranian.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import com.sojka.pomeranian.lib.dto.UserPresenceRequest;
import com.sojka.pomeranian.lib.util.JsonUtils;
import com.sojka.pomeranian.pubsub.config.GcpConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * TODO: <br>
 *  - implement a meaningful callback <br>
 *  - This is mostly duplicated with {@link }, create some common abstract class <br>
 */
@Slf4j
@Component
public class UserPresencePublisher {

    public static final String ENDPOINT_FORMAT = "%s-pubsub.googleapis.com:443";

    private final Publisher publisher;

    public UserPresencePublisher(GcpConfig gcpConfig) throws IOException {
        TopicName topic = TopicName.of(gcpConfig.getProjectId(), gcpConfig.getUserPresenceConfig().getTopicName());
        this.publisher = Publisher.newBuilder(topic)
                .setEndpoint(ENDPOINT_FORMAT.formatted(gcpConfig.getRegion()))
                .setEnableMessageOrdering(true) // TODO: DIFFERENCE message ordering enabled
                .build();
    }

    public ApiFuture<String> publish(UserPresenceRequest request) {
        var future = publisher.publish(PubsubMessage.newBuilder()
                .setOrderingKey(request.userId().toString())// TODO: DIFFERENCE message ordering enabled
                .setData(toData(request))
                .build());

        ApiFutures.addCallback(
                future,
                new ApiFutureCallback<>() {

                    @Override
                    public void onFailure(Throwable throwable) {
                        if (throwable instanceof ApiException apiException) {
                            // details on the API exception
                            System.out.println(apiException.getStatusCode().getCode());
                            System.out.println(apiException.isRetryable());
                        }
                        log.error("Error publishing error", throwable);
                    }

                    @Override
                    public void onSuccess(String messageId) {
                        log.info("Published message ID: {}", messageId);
                    }
                },
                MoreExecutors.directExecutor());

        return future;
    }

    // TODO: move to lib
    ByteString toData(Object object) {
        try {
            return ByteString.copyFrom(JsonUtils.getWriter().writeValueAsBytes(object));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}

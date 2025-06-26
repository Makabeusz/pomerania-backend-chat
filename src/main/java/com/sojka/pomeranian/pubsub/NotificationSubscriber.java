package com.sojka.pomeranian.pubsub;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.sojka.pomeranian.chat.util.JsonUtils;
import com.sojka.pomeranian.notification.dto.NotificationDto;
import com.sojka.pomeranian.notification.service.NotificationService;
import com.sojka.pomeranian.pubsub.config.GcpConfig;
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
                ProjectSubscriptionName.of(gcpConfig.getProjectId(), gcpConfig.getNotificationsConfig().getSubscriptionName());

        MessageReceiver receiver =
                (PubsubMessage message, AckReplyConsumer consumer) -> {
                    var notification = JsonUtils.readObject(message.getData().toByteArray(), NotificationDto.class);

                    notificationService.publish(notification);
                    consumer.ack();


                    // todo
                    //  2025-06-16T12:06:55.733+02:00  INFO 7700 --- [pomeranian-chat] [          Gax-1] c.s.p.n.service.NotificationService      : Published notification: NotificationResponse(data=NotificationDto(profileId=b3d36d79-4307-4123-8be5-3cfe42f5d0a7, createdAt=2025-06-16T10:06:55.569, type=FOLLOW, readAt=null, relatedId=null, content=You have been followed by bbb, senderId=null, senderUsername=null, metadata={username=bbb, id=57bab9b4-6368-4014-88fa-18a0dbca4372}), type=FOLLOW)
                    //2025-06-16T12:06:56.749+02:00  WARN 7700 --- [pomeranian-chat] [bscriber-SE-1-5] c.g.c.p.v.StreamingSubscriberConnection  : failed to send operations
                    //
                    //com.google.api.gax.rpc.InvalidArgumentException: io.grpc.StatusRuntimeException: INVALID_ARGUMENT: Some acknowledgement ids in the request were invalid. This could be because the acknowledgement ids have expired or the acknowledgement ids were malformed.
                    //	at com.google.api.gax.rpc.ApiExceptionFactory.createException(ApiExceptionFactory.java:92) ~[gax-2.66.0.jar:2.66.0]
                    //	at com.google.api.gax.grpc.GrpcApiExceptionFactory.create(GrpcApiExceptionFactory.java:98) ~[gax-grpc-2.66.0.jar:2.66.0]
                    //	at com.google.api.gax.grpc.GrpcApiExceptionFactory.create(GrpcApiExceptionFactory.java:66) ~[gax-grpc-2.66.0.jar:2.66.0]
                    //	at com.google.api.gax.grpc.GrpcExceptionCallable$ExceptionTransformingFuture.onFailure(GrpcExceptionCallable.java:97) ~[gax-grpc-2.66.0.jar:2.66.0]
                    //	at com.google.api.core.ApiFutures$1.onFailure(ApiFutures.java:84) ~[api-common-2.49.0.jar:2.49.0]
                    //	at com.google.common.util.concurrent.Futures$CallbackListener.run(Futures.java:1132) ~[guava-33.4.0-jre.jar:na]
                    //	at com.google.common.util.concurrent.DirectExecutor.execute(DirectExecutor.java:31) ~[guava-33.4.0-jre.jar:na]
                    //	at com.google.common.util.concurrent.AbstractFuture.executeListener(AbstractFuture.java:1307) ~[guava-33.4.0-jre.jar:na]
                    //	at com.google.common.util.concurrent.AbstractFuture.complete(AbstractFuture.java:1070) ~[guava-33.4.0-jre.jar:na]
                    //	at com.google.common.util.concurrent.AbstractFuture.setException(AbstractFuture.java:819) ~[guava-33.4.0-jre.jar:na]
                    //	at io.grpc.stub.ClientCalls$GrpcFuture.setException(ClientCalls.java:651) ~[grpc-stub-1.70.0.jar:1.70.0]
                    //	at io.grpc.stub.ClientCalls$UnaryStreamToFuture.onClose(ClientCalls.java:621) ~[grpc-stub-1.70.0.jar:1.70.0]
                    //	at io.grpc.PartialForwardingClientCallListener.onClose(PartialForwardingClientCallListener.java:39) ~[grpc-api-1.70.0.jar:1.70.0]
                    //	at io.grpc.ForwardingClientCallListener.onClose(ForwardingClientCallListener.java:23) ~[grpc-api-1.70.0.jar:1.70.0]
                    //	at io.grpc.ForwardingClientCallListener$SimpleForwardingClientCallListener.onClose(ForwardingClientCallListener.java:40) ~[grpc-api-1.70.0.jar:1.70.0]
                    //	at com.google.api.gax.grpc.ChannelPool$ReleasingClientCall$1.onClose(ChannelPool.java:569) ~[gax-grpc-2.66.0.jar:2.66.0]
                    //	at io.grpc.PartialForwardingClientCallListener.onClose(PartialForwardingClientCallListener.java:39) ~[grpc-api-1.70.0.jar:1.70.0]
                    //	at io.grpc.ForwardingClientCallListener.onClose(ForwardingClientCallListener.java:23) ~[grpc-api-1.70.0.jar:1.70.0]
                    //	at io.grpc.ForwardingClientCallListener$SimpleForwardingClientCallListener.onClose(ForwardingClientCallListener.java:40) ~[grpc-api-1.70.0.jar:1.70.0]
                    //	at com.google.api.gax.grpc.GrpcLoggingInterceptor$1$1.onClose(GrpcLoggingInterceptor.java:98) ~[gax-grpc-2.66.0.jar:2.66.0]
                    //	at io.grpc.internal.ClientCallImpl.closeObserver(ClientCallImpl.java:564) ~[grpc-core-1.70.0.jar:1.70.0]
                    //	at io.grpc.internal.ClientCallImpl.access$100(ClientCallImpl.java:72) ~[grpc-core-1.70.0.jar:1.70.0]
                    //	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInternal(ClientCallImpl.java:729) ~[grpc-core-1.70.0.jar:1.70.0]
                    //	at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInContext(ClientCallImpl.java:710) ~[grpc-core-1.70.0.jar:1.70.0]
                    //	at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37) ~[grpc-core-1.70.0.jar:1.70.0]
                    //	at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133) ~[grpc-core-1.70.0.jar:1.70.0]
                    //	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:572) ~[na:na]
                    //	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317) ~[na:na]
                    //	at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:304) ~[na:na]
                    //	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144) ~[na:na]
                    //	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642) ~[na:na]
                    //	at java.base/java.lang.Thread.run(Thread.java:1583) ~[na:na]
                    //Caused by: io.grpc.StatusRuntimeException: INVALID_ARGUMENT: Some acknowledgement ids in the request were invalid. This could be because the acknowledgement ids have expired or the acknowledgement ids were malformed.
                    //	at io.grpc.Status.asRuntimeException(Status.java:532) ~[grpc-api-1.70.0.jar:1.70.0]
                    //	... 21 common frames omitted
                    //
                    //2025-06-16T12:06:56.814+02:00  INFO 7700 --- [pomeranian-chat] [bscriber-SE-1-5] c.g.c.p.v.StreamingSubscriberConnection  : Permanent error invalid ack id message, will not resend
                    //Caused by: io.grpc.StatusRuntimeException: INVALID_ARGUMENT: Some acknowledgement ids in the request were invalid. This could be because the acknowledgement ids have expired or the acknowledgement ids were malformed.
                };

        subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();

        // Start the subscriber.
        subscriber.startAsync().awaitRunning();
        log.info("Listening for messages on {}", subscriptionName);

        return subscriber;
    }
}

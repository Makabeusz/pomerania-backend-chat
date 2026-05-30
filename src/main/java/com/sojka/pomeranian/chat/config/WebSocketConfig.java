package com.sojka.pomeranian.chat.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;

import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompRequestAuthenticator requestAuthenticator;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry
                .setUserDestinationPrefix("/user")
                .setApplicationDestinationPrefixes("/app")
                .enableSimpleBroker("/user", "/queue");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 1. Custom JWT authentication interceptor.
        // Only performs real work on CONNECT (sets the user principal on the session).
        registration.interceptors(new JwtStompInterceptor());

        // 2. Spring Security's official interceptor for STOMP.
        // This is the key piece that makes @PreAuthorize("@authx.isLoggedIn(authentication)")
        // and @AuthenticationPrincipal work on @MessageMapping methods.
        // It properly restores the SecurityContext from the authenticated STOMP session
        // even when messages are processed asynchronously.
        registration.interceptors(new SecurityContextChannelInterceptor());
    }

    public class JwtStompInterceptor implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            log.trace("Stomp auth attempt, command={}", accessor.getCommand());

            // Only perform full JWT authentication on CONNECT.
            // For subsequent messages (SEND, SUBSCRIBE, etc.), we rely on the principal
            // that was established during CONNECT + SecurityContextChannelInterceptor.
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                requestAuthenticator.authenticate(accessor);
            }

            return message;
        }
    }

}
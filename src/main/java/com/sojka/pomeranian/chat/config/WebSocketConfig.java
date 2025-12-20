package com.sojka.pomeranian.chat.config;

import com.sojka.pomeranian.security.model.Role;
import com.sojka.pomeranian.security.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Set;

import static com.sojka.pomeranian.lib.util.CommonUtils.getAuthUser;
import static com.sojka.pomeranian.security.model.Role.PomeranianRole.ADMIN;
import static com.sojka.pomeranian.security.model.Role.PomeranianRole.SOFT_BAN;
import static com.sojka.pomeranian.security.model.Role.PomeranianRole.USER;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final Set<Role.PomeranianRole> allowedRoles = Set.of(ADMIN, USER, SOFT_BAN);

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
        registration.interceptors(new JwtStompInterceptor());
    }

    public class JwtStompInterceptor implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                Object principal = accessor.getHeader("simpUser");
                if (principal == null) {
                    throw new SecurityException("WebSocket connection attempt without principal");
                }
                User user = getAuthUser(principal);

                if (!allowedRoles.contains(user.getRole())) {
                    throw new MessagingException("Role not allowed to connect: role=%s, username=%s"
                            .formatted(user.getRole(), user.getUsername()));
                }
            }

            return message;
        }
    }
}
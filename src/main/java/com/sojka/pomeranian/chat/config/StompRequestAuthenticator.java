package com.sojka.pomeranian.chat.config;

import com.sojka.pomeranian.security.model.Role;
import com.sojka.pomeranian.security.model.User;
import com.sojka.pomeranian.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;

import java.util.List;
import java.util.Map;

/**
 * Handles JWT-based authentication for STOMP/WebSocket connections.
 *
 * <p>Architecture:
 * <ul>
 *   <li>On CONNECT: validate the JWT and create an {@link Authentication} with the real {@link User}
 *       as the principal. The Authentication is set on the STOMP session via {@code accessor.setUser()}.</li>
 *   <li>The {@link org.springframework.security.messaging.context.SecurityContextChannelInterceptor}
 *       (registered in WebSocketConfig) ensures that subsequent STOMP messages have a proper
 *       {@code Authentication} in the {@code SecurityContext}, allowing
 *       {@code @AuthenticationPrincipal UsernamePasswordAuthenticationToken} and
 *       {@code @PreAuthorize("@authx.isLoggedIn(authentication))"} to work on @MessageMapping methods.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompRequestAuthenticator {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    private String getAuthJwt(StompHeaderAccessor accessor) {
        var list = accessor.getNativeHeader("auth_jwt");
        if (list != null && !list.isEmpty()) {
            return list.getFirst();
        }

        // Fallback to internal Spring STOMP connect message (brittle but currently required)
        Object simpConnectMessage = accessor.getMessageHeaders().get("simpConnectMessage");
        if (simpConnectMessage instanceof GenericMessage simpConnectMessageHeader) {
            var nativeHeaders = simpConnectMessageHeader.getHeaders().get("nativeHeaders");
            if (nativeHeaders instanceof Map nativeHeadersMap) {
                List<String> authJwt = (List<String>) nativeHeadersMap.get("auth_jwt");
                if (authJwt != null && !authJwt.isEmpty()) {
                    return authJwt.getFirst();
                }
            }
        }

        log.debug("No auth_jwt found in STOMP headers");
        return null;
    }

    /**
     * Obtains the authenticated user from an AbstractSubProtocolEvent.
     * Prefers the SecurityContext (populated by SecurityContextChannelInterceptor).
     */
    public User getUser(AbstractSubProtocolEvent event, StompHeaderAccessor accessor) throws SecurityException {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.getPrincipal() instanceof User user) {
            return user;
        }

        Object principal = event.getUser();

        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof UsernamePasswordAuthenticationToken token &&
            token.getPrincipal() instanceof User user) {
            return user;
        }
        return getUser(accessor);
    }

    public User getUser(AbstractSubProtocolEvent event) throws SecurityException {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.getPrincipal() instanceof User user) {
            return user;
        }

        Object principal = event.getUser();

        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof UsernamePasswordAuthenticationToken token &&
            token.getPrincipal() instanceof User user) {
            return user;
        }
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        return getUser(accessor);
    }

    /**
     * Obtains the authenticated user for STOMP handlers.
     * Prefers the SecurityContext (populated by SecurityContextChannelInterceptor).
     * Falls back to the session accessor or JWT re-validation.
     */
    public User getUser(StompHeaderAccessor accessor) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.getPrincipal() instanceof User user) {
            return user;
        }

        Object userObject = accessor.getUser();

        if (userObject instanceof Authentication auth && auth.getPrincipal() instanceof User user) {
            return user;
        }
        if (userObject instanceof User user) {
            return user;
        }

        // Final fallback: re-validate using the JWT from headers
        try {
            String authJwt = getAuthJwt(accessor);
            if (authJwt == null || authJwt.isBlank()) {
                throw new SecurityException("Missing or empty auth_jwt");
            }
            var username = jwtUtil.extractUsername(authJwt);
            var userDetails = userDetailsService.loadUserByUsername(username);

            User user = (User) userDetails;
            if (isUserBlocked(user)) {
                throw new SecurityException("User is not allowed to connect: " + user.getRole());
            }
            return user;
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("STOMP authentication failed", e);
        }
    }

    public User authenticate(StompHeaderAccessor accessor) {
        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return null;
        }

        String authJwt = getAuthJwt(accessor);
        if (authJwt == null || authJwt.isBlank()) {
            throw new SecurityException("Missing or empty auth_jwt on CONNECT");
        }

        var username = jwtUtil.extractUsername(authJwt);
        var userDetails = userDetailsService.loadUserByUsername(username);

        User user = (User) userDetails;
        if (isUserBlocked(user)) {
            throw new SecurityException("User is not allowed to connect: " + user.getRole());
        }

        // Create Authentication with the real User as principal.
        // We set the Authentication object itself via setUser().
        // SecurityContextChannelInterceptor will then make an Authentication
        // whose .getPrincipal() is the real User available in the SecurityContext
        // for @MessageMapping methods. This makes @AuthenticationPrincipal User user
        // behave the same way as on HTTP endpoints.
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.setContext(
                SecurityContextHolder.createEmptyContext()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        accessor.setUser(authentication);
        return user;
    }

    private boolean isUserBlocked(User user) {
        if (user == null) {
            return true;
        }
        var role = user.getRole();
        return role == Role.PomeranianRole.DEACTIVATED
                || role == Role.PomeranianRole.HARD_BAN;
    }
}

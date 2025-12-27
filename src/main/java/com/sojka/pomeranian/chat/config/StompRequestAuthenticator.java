package com.sojka.pomeranian.chat.config;

import com.sojka.pomeranian.security.model.User;
import com.sojka.pomeranian.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompRequestAuthenticator {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    // TODO: allow auth if roles are ok
//    private final Set<Role.PomeranianRole> allowedRoles = Set.of(ADMIN, USER, SOFT_BAN);

    private String getAuthJwt(StompHeaderAccessor accessor) {
        var list = accessor.getNativeHeader("auth_jwt");
        if (list != null && !list.isEmpty()) {
            return list.getFirst();
        }

        Object simpConnectMessage = accessor.getMessageHeaders().get("simpConnectMessage");
        if (simpConnectMessage instanceof GenericMessage simpConnectMessageHeader) {
            var nativeHeaders = simpConnectMessageHeader.getHeaders().get("nativeHeaders");
            if (nativeHeaders instanceof Map nativeHeadersMap) {
                List<String> authJwt = (List<String>) nativeHeadersMap.get("auth_jwt");
                if (authJwt != null || !authJwt.isEmpty()) {
                    return authJwt.getFirst();
                }
            }
        }
        return null;
    }

    public User getUser(AbstractSubProtocolEvent event) throws SecurityException {
        if (event.getUser() instanceof UsernamePasswordAuthenticationToken token) {
            return (User) token.getPrincipal();
        } else if (event.getUser() instanceof User user) {
            return user;
        }
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        return getUser(accessor);
    }

    public User getUser(StompHeaderAccessor accessor) {
        try {
            String authJwt = getAuthJwt(accessor);
            var username = jwtUtil.extractUsername(authJwt);
            var userDetails = userDetailsService.loadUserByUsername(username);
            return (User) userDetails;
        } catch (Exception e) {
            throw new SecurityException(e);
        }
    }

    public User authenticate(StompHeaderAccessor accessor) {
        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return null;
        }

        String authJwt = getAuthJwt(accessor);

        var username = jwtUtil.extractUsername(authJwt);
        var userDetails = userDetailsService.loadUserByUsername(username);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.setContext(
                SecurityContextHolder.createEmptyContext()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        SecurityContextHolder.setStrategyName(
                SecurityContextHolder.MODE_THREADLOCAL
        );

        // Also set on accessor for STOMP-specific access (good practice)
        accessor.setUser(authentication);
        return (User) userDetails;
    }
}

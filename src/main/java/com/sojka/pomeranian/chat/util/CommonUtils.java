package com.sojka.pomeranian.chat.util;

import com.sojka.pomeranian.chat.dto.ChatMessage;
import com.sojka.pomeranian.security.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public final class CommonUtils {

    /**
     * @see CommonUtils#generateRoomId(String, String)
     */
    public static String generateRoomId(ChatMessage chatMessage) {
        return CommonUtils.generateRoomId(chatMessage.getSender().id(), chatMessage.getRecipient().id());
    }

    /**
     * Generates consistent private message room ID.<br>
     * Sorts the IDs in alphabetic order and combine them with a colon ':'.
     */
    public static String generateRoomId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0 ? userId1 + ":" + userId2 : userId2 + ":" + userId1;
    }

    /**
     * Returns standardised {@link Instant}, truncated to milliseconds for all databases compatibility.
     */
    public static Instant getCurrentInstant() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /**
     * TODO: return epoch seconds or millis and create a date in the frontend.
     */
    public static String formatToDateString(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * TODO: return epoch seconds or millis and create a date in the frontend.
     */
    public static Instant formatToInstant(String datetime) {
        if (datetime == null) {
            return null;
        }
        return Instant.parse(datetime + "Z");
    }

    public static User getAuthUser(Principal principal) {
        try {
            if (principal instanceof UsernamePasswordAuthenticationToken p) {
                return (User) p.getPrincipal();
            } else {
                throw new SecurityException("User authentication failed: Not found principal");
            }
        } catch (Exception e) {
            throw new SecurityException("User authentication failed", e);
        }
    }

    public static String getRecipientIdFromRoomId(String roomId, String userId) {
        return Arrays.stream(roomId.split(":"))
                .filter(i -> !i.equals(userId))
                .findAny()
                .orElseThrow(() -> new SecurityException("The user_id=%s is not part of the room_id=%s"
                        .formatted(userId, roomId)));
    }
}

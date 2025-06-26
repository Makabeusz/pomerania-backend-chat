package com.sojka.pomeranian.chat.util;

/**
 * Common application constants.
 */
public final class Constants {

    private Constants() {
    }

    public static final String MESSAGES_KEYSPACE = "messages";
    public static final String NOTIFICATIONS_KEYSPACE = "notifications";
    public static final String DM_DESTINATION = "/queue/private";
    public static final String NOTIFY_DESTINATION = "/queue/notification";
    public static final String MESSAGE_DESTINATION = "/queue/comments";
}

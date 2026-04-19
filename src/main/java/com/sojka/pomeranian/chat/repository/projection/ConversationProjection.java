package com.sojka.pomeranian.chat.repository.projection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConversationProjection {

    UUID getRecipientId();

    String getRecipientUsername();

    UUID getRecipientImage192();

    String getFlag();

    Instant getLastMessageAt();

    String getContent();

    String getContentType();

    Integer getUnreadCount();

    Boolean getIsLastMessageFromUser();

    List<String> getGender();

    List<Integer> getAge();

    Instant getLastLoginAt();

    String getCityName();

    String getCountry();

    Integer getRoleId();

    /**
     * Block profile status code, maps to:<pre>
     * 1: You blocked the profile
     * -1: The profile blocked you
     * 0: no block exists
     * </pre>
     */
    Integer getBlockStatusCode();

    // TODO: AdminFlag enum from main
    String getValidationStatus();
}

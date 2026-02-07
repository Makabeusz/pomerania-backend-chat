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
}

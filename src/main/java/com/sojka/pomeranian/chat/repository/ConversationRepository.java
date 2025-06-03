package com.sojka.pomeranian.chat.repository;

import java.util.List;

@Deprecated
public interface ConversationRepository {

    List<String> findConversations(String userId);
}

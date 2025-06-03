package com.sojka.pomeranian.chat.repository;

import java.util.List;

public interface ConversationRepository {

    List<String> findConversations(String userId);
}

package com.sojka.pomeranian.chat.repository;

import com.sojka.pomeranian.chat.model.Conversation;

public interface ConversationRepository {

    Conversation save(Conversation conversation);
}

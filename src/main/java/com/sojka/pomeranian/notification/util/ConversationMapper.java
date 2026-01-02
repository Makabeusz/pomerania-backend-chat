package com.sojka.pomeranian.notification.util;

import com.sojka.pomeranian.chat.dto.ConversationDto;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.repository.projection.ConversationProjection;
import com.sojka.pomeranian.lib.dto.ChatUser;

import static com.sojka.pomeranian.lib.util.CommonUtils.getNameOrNull;

public final class ConversationMapper {

    private ConversationMapper() {
    }

    public static ConversationDto toDto(Conversation model, ChatUser recipient) {
        return ConversationDto.builder()
                .recipient(recipient)
                .flag(getNameOrNull(model.getFlag()))
                .lastMessageAt(model.getLastMessageAt())
                .content(model.getContent())
                .contentType(getNameOrNull(model.getContentType()))
                .unreadCount(model.getUnreadCount())
                .build();
    }

    public static ConversationDto toDto(ConversationProjection projection) {
        return ConversationDto.builder()
                .recipient(new ChatUser(
                        projection.getRecipientId(), projection.getRecipientUsername(), projection.getRecipientImage192()
                ))
                .flag(projection.getFlag())
                .lastMessageAt(projection.getLastMessageAt())
                .content(projection.getContent())
                .contentType(projection.getContentType())
                .unreadCount(projection.getUnreadCount())
                .build();
    }
}

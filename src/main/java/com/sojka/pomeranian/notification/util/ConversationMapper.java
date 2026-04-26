package com.sojka.pomeranian.notification.util;

import com.sojka.pomeranian.chat.dto.ConversationDto;
import com.sojka.pomeranian.chat.model.Conversation;
import com.sojka.pomeranian.chat.repository.projection.ConversationProjection;
import com.sojka.pomeranian.lib.dto.UserData;
import com.sojka.pomeranian.security.model.Role;

import static com.sojka.pomeranian.lib.util.CommonUtils.getNameOrNull;

public final class ConversationMapper {

    private ConversationMapper() {
    }

    public static ConversationDto toDto(Conversation model, UserData recipient) {
        return ConversationDto.builder()
                .recipient(recipient)
                .flag(getNameOrNull(model.getFlag()))
                .lastMessageAt(model.getLastMessageAt())
                .content(model.getContent())
                .contentType(getNameOrNull(model.getContentType()))
                .unreadCount(model.getUnreadCount())
//                .isLastMessageFromUser(false)
                .build();
    }

    public static ConversationDto toDto(ConversationProjection projection) {
        return ConversationDto.builder()
                .recipient(UserData.builder()
                        .id(projection.getRecipientId())
                        .username(projection.getRecipientUsername())
                        .image192(projection.getRecipientImage192())
                        .gender(projection.getGender())
                        .role(projection.getRoleId() == null ? null : Role.PomeranianRole.fromOrdinal(projection.getRoleId()))
                        .build())
                .flag(projection.getFlag())
                .lastMessageAt(projection.getLastMessageAt())
                .content(projection.getContent())
                .contentType(projection.getContentType())
                .unreadCount(projection.getUnreadCount())
                .isLastMessageFromUser(projection.getIsLastMessageFromUser())
                .age(projection.getAge())
                .lastLoginAt(projection.getLastLoginAt())
                .location(projection.getCityName() == null ? null : new ConversationDto.OsmCityDto(
                        projection.getCityName(), projection.getCountry()
                ))
                .build();
    }
}

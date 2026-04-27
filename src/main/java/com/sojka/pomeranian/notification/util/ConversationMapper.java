package com.sojka.pomeranian.notification.util;

import com.sojka.pomeranian.chat.dto.BlockStatus;
import com.sojka.pomeranian.chat.dto.ConversationDto;
import com.sojka.pomeranian.chat.repository.projection.ConversationProjection;
import com.sojka.pomeranian.lib.dto.UserData;
import com.sojka.pomeranian.security.model.Role;

public final class ConversationMapper {

    private ConversationMapper() {
    }

    public static ConversationDto toDto(ConversationProjection projection) {
        var role = projection.getRoleId() == null
                ? null
                : Role.PomeranianRole.fromOrdinal(projection.getRoleId());

        var recipient = UserData.builder()
                .id(projection.getRecipientId())
                .username(projection.getRecipientUsername())
                .role(role);

        var builder = ConversationDto.builder()
                .flag(projection.getFlag())
                .lastMessageAt(projection.getLastMessageAt())
                .content(projection.getContent())
                .contentType(projection.getContentType())
                .unreadCount(projection.getUnreadCount())
                .isLastMessageFromUser(projection.getIsLastMessageFromUser())
                .lastLoginAt(projection.getLastLoginAt())
                .blockStatus(getBlockStatus(projection.getBlockStatusCode()));

        if (role != Role.PomeranianRole.DEACTIVATED) {
            recipient = recipient.image192(projection.getRecipientImage192())
                    .gender(projection.getGender());
            builder = builder
                    .age(projection.getAge())
                    .location(projection.getCityName() == null ? null : new ConversationDto.OsmCityDto(
                            projection.getCityName(), projection.getCountry()
                    ))
                    .validationStatus(projection.getValidationStatus());
        }

        return builder.recipient(recipient.build()).build();
    }

    // TODO: duplicated with main
    private static BlockStatus getBlockStatus(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case -1 -> BlockStatus.BLOCKED_YOU;
            case 1 -> BlockStatus.BLOCKED_BY_YOU;
            default -> null;
        };
    }
}

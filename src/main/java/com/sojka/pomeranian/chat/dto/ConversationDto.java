package com.sojka.pomeranian.chat.dto;

import com.sojka.pomeranian.lib.dto.UserData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    private UserData recipient;
    private String flag;
    private Instant lastMessageAt;
    private String content;
    private String contentType;
    private Integer unreadCount;
    private Boolean isLastMessageFromUser;
    private List<Integer> age;
    private Instant lastLoginAt;
    private OsmCityDto location;
    private BlockStatus blockStatus;
    // TODO: AdminFlag enum from main
    private String validationStatus;

    public record OsmCityDto(
            String cityName, String country
    ) {
    }
}

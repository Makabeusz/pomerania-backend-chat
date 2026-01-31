package com.sojka.pomeranian.chat.dto;

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

    private ChatUser recipient;
    private String flag;
    private Instant lastMessageAt;
    private String content;
    private String contentType;
    private Integer unreadCount;
    private Boolean isLastMessageFromUser;
    private List<String> gender;
    private List<Integer> age;
    private Instant lastLoginAt;
    private OsmCityDto location;
    private String cityName;
    private String country;

    public record OsmCityDto(
            String cityName, String country
    ) {
    }
}

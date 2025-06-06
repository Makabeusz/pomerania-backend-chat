package com.sojka.pomeranian.chat.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sojka.pomeranian.chat.dto.Pagination;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class PaginationMapper {

    public static String toEncodedString(Pagination pagination) {
        try {
            byte[] bytes = JsonMapper.getWriter().writeValueAsBytes(pagination);

            return new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Pagination toPagination(String encoded) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);

            return JsonMapper.getReader().readValue(decoded, Pagination.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

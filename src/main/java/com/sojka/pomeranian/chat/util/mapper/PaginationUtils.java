package com.sojka.pomeranian.chat.util.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sojka.pomeranian.chat.dto.Pagination;
import com.sojka.pomeranian.chat.util.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * TODO: duplicated with main
 */
public final class PaginationUtils {

    private PaginationUtils() {
    }

    public static String toEncodedString(Pagination pagination) {
        try {
            byte[] bytes = JsonUtils.getWriter().writeValueAsBytes(pagination);

            return new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Pagination toPagination(String encoded) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);

            return JsonUtils.getReader().readValue(decoded, Pagination.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Pagination pageStateToPagination(String pageState, int pageSize) {
        Pagination pagination;
        if (pageState != null) {
            pagination = toPagination(pageState);
        } else {
            pagination = new Pagination(0, pageSize);
        }
        return pagination;
    }

    public static String createPageState(int currentPageSize, int paginationSize, Pagination previous) {
        return currentPageSize == paginationSize
                ? toEncodedString(new Pagination(previous.pageNumber() + 1, previous.pageSize()))
                : null;
    }
}

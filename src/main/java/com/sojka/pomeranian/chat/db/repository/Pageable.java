package com.sojka.pomeranian.chat.db.repository;

public record Pageable(
        long from,
        long to
) {
}

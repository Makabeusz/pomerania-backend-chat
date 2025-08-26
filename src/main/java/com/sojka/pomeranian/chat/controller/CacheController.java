package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.chat.model.ActiveUser;
import com.sojka.pomeranian.chat.service.ChatCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final ChatCache cache;

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ActiveUser> get(@PathVariable("userId") String userId) {
        var activeUser = cache.get(userId);
        return activeUser.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.ok(null));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ActiveUser>> getAll() {
        return ResponseEntity.ok(cache.getAll());
    }
}

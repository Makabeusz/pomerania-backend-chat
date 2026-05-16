package com.sojka.pomeranian.notification.controller;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.lib.dto.Notification;
import com.sojka.pomeranian.lib.dto.NotificationType;
import com.sojka.pomeranian.notification.service.NotificationService;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;

    @GetMapping("/unread")
    @PreAuthorize("hasRole('SOFT_BAN')")
    public ResponseEntity<ResultsPage<Notification<Object>>> getUnreadNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String nextPageState) {
        var results = notificationService.getUnread(user.getId(), nextPageState, 10);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/read")
    @PreAuthorize("hasRole('SOFT_BAN')")
    public ResponseEntity<ResultsPage<Notification<Object>>> getReadNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String nextPageState) {
        var results = notificationService.getRead(user.getId(), nextPageState, 10);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('SOFT_BAN')")
    public ResponseEntity<Long> count(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.countUnreadNotifications(user.getId()));
    }

    @DeleteMapping("/read")
    @PreAuthorize("hasRole('SOFT_BAN')")
    public ResponseEntity<Boolean> deleteReadNotification(
            @AuthenticationPrincipal User user,
            @RequestParam Instant createdAt,
            @RequestParam NotificationType type
    ) {
        notificationService.deleteRead(user.getId(), createdAt, type);
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/unread")
    @PreAuthorize("hasRole('SOFT_BAN')")
    public ResponseEntity<Boolean> deleteUnreadNotification(
            @AuthenticationPrincipal User user,
            @RequestParam Instant createdAt,
            @RequestParam NotificationType type
    ) {
        notificationService.deleteUnread(user.getId(), createdAt, type);
        return ResponseEntity.ok(true);
    }
}

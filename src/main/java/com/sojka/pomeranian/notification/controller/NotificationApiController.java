package com.sojka.pomeranian.notification.controller;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.notification.dto.NotificationDto;
import com.sojka.pomeranian.notification.service.NotificationService;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;

    @GetMapping("/unread")
    public ResponseEntity<ResultsPage<NotificationDto>> getUnreadNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String nextPageState) {
        var results = notificationService.getUnread(user.getId(), nextPageState, 10);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/read")
    public ResponseEntity<ResultsPage<NotificationDto>> getReadNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String nextPageState) {
        var results = notificationService.getRead(user.getId(), nextPageState, 10);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> count(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.countUnreadNotifications(user.getId()));
    }
}

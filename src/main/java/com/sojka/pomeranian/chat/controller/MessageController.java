package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.lib.dto.Pagination;
import com.sojka.pomeranian.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatService chatService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ResultsPage<ChatMessagePersisted>> getConversation(
            @RequestParam String recipientId,
            @RequestParam(required = false) String nextPageState,
            @AuthenticationPrincipal User user
    ) {
        log.trace("getConversation input: recipientId={}, nextPageState={}", recipientId, nextPageState);
        return ResponseEntity.ok(chatService.getConversation(user.getId(), recipientId, nextPageState));
    }

    @GetMapping("/headers")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ResultsPage<ChatMessagePersisted>> getConversationHeaders(
            @AuthenticationPrincipal User user,
            @RequestParam int pageNumber,
            @RequestParam int pageSize
    ) {
        return ResponseEntity.ok(chatService.getConversationHeaders(user.getId(), new Pagination(pageNumber, pageSize)));
    }

    @PostMapping("/headers")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Boolean> updateConversationFlag(
            @AuthenticationPrincipal User user,
            @RequestParam String recipientId,
            @RequestParam Boolean flag) {
        log.trace("updateConversationFlag input: userID={}, recipientId={}, flag={}",
                user.getId(), recipientId, flag);
        return ResponseEntity.ok(chatService.updateConversationFlag(user.getId(), recipientId, flag));
    }

    @GetMapping("/headers/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getConversationHeadersCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getConversationsHeadersCount(user.getId()));
    }

    @DeleteMapping("/resource")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Boolean> deleteResource(
            @AuthenticationPrincipal User user,
            @RequestParam String roomId,
            @RequestParam String createdAt,
            @RequestParam String profileId
    ) {
        return ResponseEntity.ok(chatService.deleteMessageResource(roomId, createdAt, profileId, user.getId()));
    }

}

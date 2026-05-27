package com.sojka.pomeranian.chat.controller;

import com.sojka.pomeranian.astra.dto.ResultsPage;
import com.sojka.pomeranian.chat.dto.ChatMessagePersisted;
import com.sojka.pomeranian.chat.dto.ConversationDto;
import com.sojka.pomeranian.chat.service.ChatService;
import com.sojka.pomeranian.lib.dto.ConversationFlag;
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

import java.util.List;
import java.util.UUID;

// TODO: refactor most or all of it to websockets - connection is already here, HTTP is bad here.
@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatService chatService;

    @GetMapping
    @PreAuthorize("@authx.isLoggedIn(authentication)")
    public ResponseEntity<ResultsPage<ChatMessagePersisted>> getConversation(
            @RequestParam UUID recipientId,
            @RequestParam(required = false) String nextPageState,
            @AuthenticationPrincipal User user
    ) {
        log.trace("getConversation input: recipientId={}, nextPageState={}", recipientId, nextPageState);
        return ResponseEntity.ok(chatService.getConversationMessages(user.getId(), recipientId, nextPageState));
    }

    @GetMapping("/headers")
    @PreAuthorize("@authx.isLoggedIn(authentication)")
    public ResponseEntity<List<ConversationDto>> getConversations(
            @AuthenticationPrincipal User user,
            @RequestParam int pageNumber,
            @RequestParam int pageSize,
            @RequestParam ConversationFlag flag
    ) {
        var conversations = chatService.getConversations(user.getId(), flag, new Pagination(pageNumber, pageSize));
        return ResponseEntity.ok(conversations);
    }

    // TODO: rename those "headers" to conversations everywhere
    @PostMapping("/headers")
    @PreAuthorize("@authx.isLoggedIn(authentication)")
    public ResponseEntity<Boolean> updateConversationFlag(
            @AuthenticationPrincipal User user,
            @RequestParam UUID recipientId,
            @RequestParam ConversationFlag flag
    ) {
        log.trace("updateConversationFlag input: userID={}, recipientId={}, flag={}", user.getId(), recipientId, flag);
        return ResponseEntity.ok(chatService.updateConversationFlag(user.getId(), recipientId, flag));
    }

    @GetMapping("/headers/count")
    @PreAuthorize("@authx.isLoggedIn(authentication)")
    public ResponseEntity<Long> getConversationCount(
            @AuthenticationPrincipal User user,
            @RequestParam ConversationFlag flag
    ) {
        log.trace("getConversationCount input: userID={}, flag={}", user.getId(), flag);
        return ResponseEntity.ok(chatService.getConversationsCount(user.getId(), flag));
    }

    @DeleteMapping("/resource")
    @PreAuthorize("@authx.isLoggedIn(authentication)")
    public ResponseEntity<Boolean> deleteResource(
            @AuthenticationPrincipal User user,
            @RequestParam String roomId,
            @RequestParam String createdAt
    ) {
        return ResponseEntity.ok(chatService.deleteMessageResource(roomId, createdAt, user.getId()));
    }

}

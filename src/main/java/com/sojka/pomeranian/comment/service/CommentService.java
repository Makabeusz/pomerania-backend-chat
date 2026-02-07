package com.sojka.pomeranian.comment.service;

import com.sojka.pomeranian.chat.service.cache.SessionCache;
import com.sojka.pomeranian.chat.util.mapper.NotificationMapper;
import com.sojka.pomeranian.lib.dto.CommentStompRequest;
import com.sojka.pomeranian.notification.service.NotificationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.sojka.pomeranian.chat.dto.StompSubscription.Type.POST_COMMENTS;
import static com.sojka.pomeranian.chat.util.Constants.COMMENTS_DESTINATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final SessionCache cache;

    public void process(CommentStompRequest dto) {
        // comments section update
        messagingTemplate.convertAndSendToUser(
                dto.getElement().getId() + "", COMMENTS_DESTINATION, CommentResponse.from(dto));
        // TODO: should check instead if given StompSubscription (POST_COMMENTS + postId) is online
        if (dto.isPublishNotification() && !cache.isOnline(dto.getElement().getOwner().getId(), POST_COMMENTS)) {
            notificationService.process(NotificationMapper.toNotification(dto));
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentResponse {

        private UUID id;
        private UUID profileId;
        private String content;
        private String createdAt; // TODO: change to instant
        private String updatedAt;
        private String username;
        private UUID image192;
        private PairCommenter commenter;

        private UUID relatedId;

        public static CommentResponse from(CommentStompRequest request) {
            var commenter = request.getCommenterId() == null ? null : PairCommenter.builder()
                    .id(request.getCommenterId())
                    .name(request.getCommenterName())
                    .build();
            return CommentResponse.builder()
                    .id(request.getId())
                    .profileId(request.getSender().getId())
                    .content(request.getContent())
                    .createdAt(request.getCreatedAt())
                    .updatedAt(request.getUpdatedAt())
                    .username(request.getSender().getUsername())
                    .image192(request.getSender().getImage192())
                    .commenter(commenter)
                    .relatedId(request.getElement().getId())
                    .build();
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PairCommenter {
            private UUID id;
            private String name;
        }
    }
}

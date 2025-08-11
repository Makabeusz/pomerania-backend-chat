package com.sojka.pomeranian.comment;

/**
 * TODO: duplicated with main
 */
public record CommentStompRequest(
        String id,
        String relatedId,
        String relatedType,
        String profileId,
        String content,
        String createdAt,
        String updatedAt,
        String username,
        String image192,
        String authorFirstname
) {
}

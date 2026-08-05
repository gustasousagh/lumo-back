package com.movies.backend.post.response;

import com.movies.backend.user.response.UserMiniResponse;
import java.time.Instant;

/**
 * Uma publicação do feed, com autor, mídia opcional (resenha), contagens de
 * curtidas/comentários e se o usuário atual já curtiu.
 */
public record PostResponse(
        Long id,
        UserMiniResponse author,
        String text,
        String kind,
        MediaRef media,
        Integer rating,
        long likeCount,
        long commentCount,
        boolean likedByMe,
        Instant createdAt
) {
    /** Mídia associada à resenha (ou null se for post de texto). */
    public record MediaRef(
            String mediaType,
            Long mediaId,
            String title,
            String posterUrl
    ) {
    }
}

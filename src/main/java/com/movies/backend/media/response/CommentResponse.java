package com.movies.backend.media.response;

import com.movies.backend.user.response.UserMiniResponse;
import java.time.Instant;

/** Um comentário com o mini perfil do autor. */
public record CommentResponse(
        Long id,
        String text,
        UserMiniResponse author,
        Instant createdAt
) {
}

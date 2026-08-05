package com.movies.backend.post.response;

import com.movies.backend.user.response.UserMiniResponse;
import java.time.Instant;

/** Um comentário de publicação com o mini perfil do autor. */
public record PostCommentResponse(
        Long id,
        UserMiniResponse author,
        String text,
        Instant createdAt
) {
}

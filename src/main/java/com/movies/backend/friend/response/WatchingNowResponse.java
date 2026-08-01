package com.movies.backend.friend.response;

import com.movies.backend.user.entity.User;
import java.time.Instant;

/** O que um usuário está "assistindo agora" (ou null se nada). */
public record WatchingNowResponse(
        String title,
        String posterUrl,
        Long mediaId,
        String mediaType,
        Instant updatedAt
) {
    /** Monta a partir do User, ou retorna null se não houver nada em andamento. */
    public static WatchingNowResponse from(User user) {
        if (user.getWatchingTitle() == null && user.getWatchingMediaId() == null) {
            return null;
        }
        return new WatchingNowResponse(
                user.getWatchingTitle(),
                user.getWatchingPosterUrl(),
                user.getWatchingMediaId(),
                user.getWatchingMediaType(),
                user.getWatchingUpdatedAt());
    }
}

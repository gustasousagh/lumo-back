package com.movies.backend.user.response;

import com.movies.backend.friend.response.WatchingNowResponse;
import java.time.Instant;

/**
 * Perfil (próprio ou de outra pessoa). O email só é preenchido no perfil próprio.
 * friendStatus: none|pending_out|pending_in|friends|self (relativo a quem consulta).
 */
public record ProfileResponse(
        Long id,
        String name,
        String username,
        String email,
        String bio,
        String avatarUrl,
        String coverUrl,
        String accent,
        Instant createdAt,
        long friendCount,
        String friendStatus,
        WatchingNowResponse watchingNow
) {
}

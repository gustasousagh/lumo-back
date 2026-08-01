package com.movies.backend.friend.response;

/** Um amigo na listagem /api/friends. */
public record FriendResponse(
        Long id,
        String name,
        String username,
        String avatarUrl,
        String accent,
        boolean online,
        WatchingNowResponse watchingNow
) {
}

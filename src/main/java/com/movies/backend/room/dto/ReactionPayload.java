package com.movies.backend.room.dto;

/** Payload de reação flutuante (WebSocket). */
public record ReactionPayload(
        String emoji
) {
}

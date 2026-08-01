package com.movies.backend.room.dto;

/** Payload de mensagem de chat (WebSocket). */
public record ChatPayload(
        String text
) {
}

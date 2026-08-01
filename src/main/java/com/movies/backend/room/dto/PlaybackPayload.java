package com.movies.backend.room.dto;

/** Payload de controle de playback (WebSocket). */
public record PlaybackPayload(
        boolean isPlaying,
        double currentTimeSec
) {
}

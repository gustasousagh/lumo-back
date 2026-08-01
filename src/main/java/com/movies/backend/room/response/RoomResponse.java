package com.movies.backend.room.response;

import com.movies.backend.media.response.MediaCatalogResponse;
import java.time.Instant;
import java.util.List;

/** Estado completo de uma sala. */
public record RoomResponse(
        Long id,
        String code,
        String status,
        MediaCatalogResponse media,
        boolean isPlaying,
        double currentTimeSec,
        Instant playbackUpdatedAt,
        Integer seasonNumber,
        Integer episodeNumber,
        List<ParticipantResponse> participants,
        boolean amIHost,
        Instant createdAt
) {
}

package com.movies.backend.progress.response;

import com.movies.backend.progress.entity.WatchProgress;
import java.time.Instant;

/** Um item de "continue assistindo". */
public record ProgressResponse(
        Long id,
        String mediaType,
        Long mediaId,
        String contentKind,
        String title,
        String posterUrl,
        Integer seasonNumber,
        Integer episodeNumber,
        double progressSec,
        Double durationSec,
        Instant updatedAt
) {
    public static ProgressResponse from(WatchProgress p) {
        return new ProgressResponse(
                p.getId(),
                p.getMediaType(),
                p.getMediaId(),
                p.getContentKind(),
                p.getTitle(),
                p.getPosterUrl(),
                p.getSeasonNumber(),
                p.getEpisodeNumber(),
                p.getProgressSec(),
                p.getDurationSec(),
                p.getUpdatedAt());
    }
}

package com.movies.backend.room.dto;

import com.movies.backend.media.dto.MediaSnapshotRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corpo do POST /api/rooms: foto da mídia + episódio opcional. */
public record CreateRoomRequest(
        @NotNull(message = "Informe o mediaId")
        Long mediaId,

        @NotBlank(message = "Informe o mediaType (movie|tv)")
        String mediaType,

        String contentKind,

        @NotBlank(message = "Informe o título")
        String title,

        String posterUrl,
        String backdropUrl,
        String releaseDate,
        Double voteAverage,
        String overview,

        Integer seasonNumber,
        Integer episodeNumber
) {
    /** Converte para a "foto" reutilizável do catálogo. */
    public MediaSnapshotRequest toSnapshot() {
        return new MediaSnapshotRequest(
                mediaId, mediaType, contentKind, title,
                posterUrl, backdropUrl, releaseDate, voteAverage, overview);
    }
}

package com.movies.backend.progress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corpo do PUT /api/progress (upsert de "continue assistindo"). */
public record UpdateProgressRequest(
        @NotBlank(message = "Informe o mediaType (movie|tv)")
        String mediaType,

        @NotNull(message = "Informe o mediaId")
        Long mediaId,

        String contentKind,

        @NotBlank(message = "Informe o título")
        String title,

        String posterUrl,
        Integer seasonNumber,
        Integer episodeNumber,

        @NotNull(message = "Informe o progresso (segundos)")
        Double progressSec,

        Double durationSec
) {
}

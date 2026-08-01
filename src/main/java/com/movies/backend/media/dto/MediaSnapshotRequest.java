package com.movies.backend.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * "Foto" de uma mídia vinda do front (dados do TMDB já resolvidos).
 * Reutilizado no upsert do catálogo, ao adicionar itens em listas e ao criar salas.
 */
public record MediaSnapshotRequest(
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
        String overview
) {
}

package com.movies.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo do PATCH /api/users/me/watching ("assistindo agora").
 * seasonNumber/episodeNumber são aceitos por compatibilidade com o front,
 * mas não são persistidos (o "assistindo agora" guarda só o título/mídia).
 */
public record UpdateWatchingRequest(
        @NotBlank(message = "Informe o mediaType (movie|tv)")
        String mediaType,

        @NotNull(message = "Informe o mediaId")
        Long mediaId,

        @NotBlank(message = "Informe o título")
        String title,

        String posterUrl,
        Integer seasonNumber,
        Integer episodeNumber
) {
}

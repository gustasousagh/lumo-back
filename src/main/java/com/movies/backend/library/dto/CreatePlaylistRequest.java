package com.movies.backend.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePlaylistRequest(
        @NotBlank(message = "Dê um nome para a playlist")
        @Size(max = 120, message = "Nome muito longo")
        String title,

        @Size(max = 500, message = "Descrição muito longa")
        String description,

        /** PRIVATE | UNLISTED | PUBLIC. Vazio = PRIVATE. */
        String visibility,

        boolean collaborative
) {
}

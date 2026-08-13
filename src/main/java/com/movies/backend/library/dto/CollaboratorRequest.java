package com.movies.backend.library.dto;

import jakarta.validation.constraints.NotBlank;

/** Convida alguém para colaborar na playlist, pelo @username. */
public record CollaboratorRequest(
        @NotBlank(message = "Informe o @usuário")
        String username
) {
}

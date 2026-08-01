package com.movies.backend.media.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo do POST de reação. */
public record ReactionRequest(
        @NotBlank(message = "Informe o emoji")
        String emoji
) {
}

package com.movies.backend.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do POST de comentário. */
public record CommentRequest(
        @NotBlank(message = "Escreva algo")
        @Size(max = 2000, message = "Comentário muito longo")
        String text
) {
}

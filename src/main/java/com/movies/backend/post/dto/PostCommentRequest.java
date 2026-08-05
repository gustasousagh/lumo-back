package com.movies.backend.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do POST de comentário em uma publicação. */
public record PostCommentRequest(
        @NotBlank(message = "Escreva algo")
        @Size(max = 1000, message = "Comentário muito longo")
        String text
) {
}

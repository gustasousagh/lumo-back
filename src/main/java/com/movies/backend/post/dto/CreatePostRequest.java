package com.movies.backend.post.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /api/posts. Exige texto não-vazio OU uma mídia anexada
 * (validado no service). rating, quando presente, deve ser de 1 a 10.
 */
public record CreatePostRequest(
        @Size(max = 1000, message = "Texto muito longo")
        String text,

        String mediaType,
        Long mediaId,
        String mediaTitle,
        String mediaPosterUrl,

        @Min(value = 1, message = "Nota deve ser entre 1 e 10")
        @Max(value = 10, message = "Nota deve ser entre 1 e 10")
        Integer rating
) {
}

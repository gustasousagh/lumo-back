package com.movies.backend.list.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do POST /api/lists. */
public record CreateListRequest(
        @NotBlank(message = "Informe o título da lista")
        @Size(max = 120, message = "Título muito longo")
        String title
) {
}

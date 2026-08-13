package com.movies.backend.library.dto;

import jakarta.validation.constraints.Min;

/** Em que página a pessoa parou. */
public record BookProgressRequest(
        @Min(value = 1, message = "Página inválida") int page
) {
}

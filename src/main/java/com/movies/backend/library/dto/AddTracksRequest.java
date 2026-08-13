package com.movies.backend.library.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Adiciona uma ou várias faixas ao fim da playlist. */
public record AddTracksRequest(
        @NotEmpty(message = "Escolha ao menos uma faixa")
        List<Long> trackIds
) {
}

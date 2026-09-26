package com.movies.backend.language.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * O resultado de uma lição, enviado pelo front ao terminar.
 *
 * Repare no que NÃO vem aqui: o XP. Quem calcula é o servidor, a partir de
 * acertos e total — senão bastaria o devtools para forjar mil pontos.
 */
public record CompleteLessonRequest(
        @NotNull Long lessonId,
        @Min(0) int correct,
        @Min(1) int total
) {}

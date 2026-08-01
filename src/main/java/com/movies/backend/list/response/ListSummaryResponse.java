package com.movies.backend.list.response;

import java.time.Instant;
import java.util.List;

/** Resumo de uma lista: contagem e até 4 pôsteres de capa. */
public record ListSummaryResponse(
        Long id,
        String title,
        long itemCount,
        List<String> coverPosterUrls,
        Instant createdAt
) {
}

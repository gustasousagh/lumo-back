package com.movies.backend.list.response;

import java.time.Instant;
import java.util.List;

/** Lista com todos os itens resolvidos. */
public record ListDetailResponse(
        Long id,
        String title,
        Instant createdAt,
        List<ListItemResponse> items
) {
}

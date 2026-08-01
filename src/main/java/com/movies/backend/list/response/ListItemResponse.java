package com.movies.backend.list.response;

import com.movies.backend.media.response.MediaCatalogResponse;
import java.time.Instant;

/** Item de lista com a mídia resolvida do catálogo. */
public record ListItemResponse(
        Long id,
        Long catalogId,
        MediaCatalogResponse media,
        Instant createdAt
) {
}

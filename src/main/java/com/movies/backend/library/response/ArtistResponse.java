package com.movies.backend.library.response;

/** Artista agregado a partir das faixas. */
public record ArtistResponse(
        String name,
        String coverUrl,
        long trackCount
) {
}

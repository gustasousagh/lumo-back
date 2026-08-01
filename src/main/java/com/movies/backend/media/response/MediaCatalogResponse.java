package com.movies.backend.media.response;

import com.movies.backend.media.entity.MediaCatalog;

/** "Foto" da mídia devolvida ao cliente. */
public record MediaCatalogResponse(
        Long id,
        Long mediaId,
        String mediaType,
        String contentKind,
        String title,
        String posterUrl,
        String backdropUrl,
        String releaseDate,
        Double voteAverage,
        String overview
) {
    public static MediaCatalogResponse from(MediaCatalog c) {
        return new MediaCatalogResponse(
                c.getId(),
                c.getMediaId(),
                c.getMediaType(),
                c.getContentKind(),
                c.getTitle(),
                c.getPosterUrl(),
                c.getBackdropUrl(),
                c.getReleaseDate(),
                c.getVoteAverage(),
                c.getOverview());
    }
}

package com.movies.backend.library.response;

import java.util.List;

/** Busca unificada: música e livro na mesma resposta. */
public record LibrarySearchResponse(
        List<TrackResponse> tracks,
        List<BookResponse> books,
        List<AlbumResponse> albums,
        List<ArtistResponse> artists,
        List<PlaylistSummaryResponse> playlists
) {
}

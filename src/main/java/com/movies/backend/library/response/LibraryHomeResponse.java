package com.movies.backend.library.response;

import java.util.List;

/**
 * A home da biblioteca em uma requisição só. Sem isso a tela abriria com cinco
 * chamadas em paralelo e cinco skeletons piscando em tempos diferentes.
 *
 * @param albums  agregado por álbum (nome, artista, capa, nº de faixas)
 * @param artists agregado por artista
 */
public record LibraryHomeResponse(
        List<TrackResponse> recentTracks,
        List<TrackResponse> topTracks,
        List<TrackResponse> favoriteTracks,
        List<BookResponse> recentBooks,
        List<BookResponse> continueReading,
        List<PlaylistSummaryResponse> myPlaylists,
        List<PlaylistSummaryResponse> sharedWithMe,
        List<PlaylistSummaryResponse> publicPlaylists,
        List<AlbumResponse> albums,
        List<ArtistResponse> artists,
        LibraryCountsResponse counts
) {
}

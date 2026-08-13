package com.movies.backend.library.response;

/** Números do topo da biblioteca. */
public record LibraryCountsResponse(
        long tracks,
        long books,
        long playlists,
        long totalDurationSec
) {
}

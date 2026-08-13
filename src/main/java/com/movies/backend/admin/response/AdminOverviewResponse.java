package com.movies.backend.admin.response;

import com.movies.backend.library.response.BookResponse;
import com.movies.backend.library.response.TrackResponse;
import java.util.List;

/**
 * O painel inteiro em uma resposta. É deliberado: o dashboard tem uma dúzia de
 * números e quatro gráficos, e servir cada um por um endpoint faria a tela abrir
 * com doze requisições e doze skeletons em tempos diferentes.
 *
 * @param signupsDaily série de cadastros por dia (últimos 30 dias, sem buracos)
 * @param playsDaily   série de reproduções por dia
 */
public record AdminOverviewResponse(
        Totals totals,
        Storage storage,
        List<DailyPoint> signupsDaily,
        List<DailyPoint> playsDaily,
        List<TrackResponse> topTracks,
        List<TrackResponse> recentTracks,
        List<BookResponse> topBooks,
        List<BookResponse> recentBooks,
        List<NameCount> topGenres,
        List<NameCount> topArtists,
        List<AdminUserResponse> newestUsers
) {
    /** Contadores gerais do sistema. */
    public record Totals(
            long users,
            long admins,
            long suspended,
            long unconfirmed,
            long newUsers30d,
            long tracks,
            long books,
            long playlists,
            long publicPlaylists,
            long posts,
            long rooms,
            long lists,
            long plays30d,
            long libraryDurationSec,
            long libraryPages
    ) {
    }

    /** Espaço ocupado e livre no disco onde a biblioteca mora. */
    public record Storage(
            long musicBytes,
            long booksBytes,
            long totalBytes,
            long freeBytes
    ) {
    }

    /** Um ponto de série temporal: "2026-08-13" -> 7. */
    public record DailyPoint(String date, long count) {
    }

    /** Par nome/contagem usado nos rankings (gêneros, artistas). */
    public record NameCount(String name, long count) {
    }
}

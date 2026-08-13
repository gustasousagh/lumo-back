package com.movies.backend.library.response;

import com.movies.backend.user.response.UserMiniResponse;
import java.time.Instant;
import java.util.List;

/**
 * Playlist na listagem (sem as faixas).
 *
 * @param coverUrls até 4 capas de faixas, para o front montar o mosaico quando
 *                  a playlist não tem capa própria
 * @param canEdit   se quem está consultando pode mexer nas faixas
 */
public record PlaylistSummaryResponse(
        Long id,
        String title,
        String description,
        String coverUrl,
        List<String> coverUrls,
        String visibility,
        String shareCode,
        boolean collaborative,
        UserMiniResponse owner,
        int trackCount,
        int durationSec,
        int collaboratorCount,
        boolean canEdit,
        boolean isOwner,
        Instant updatedAt
) {
}

package com.movies.backend.library.response;

import com.movies.backend.user.response.UserMiniResponse;
import java.time.Instant;

/**
 * Uma faixa dentro da playlist.
 *
 * <p>{@code id} é o id da LINHA da playlist, não o da faixa — é ele que o front
 * manda para remover ou reordenar. A mesma música pode aparecer duas vezes na
 * mesma playlist, então usar o id da faixa aqui removeria as duas.
 */
public record PlaylistItemResponse(
        Long id,
        int position,
        TrackResponse track,
        UserMiniResponse addedBy,
        Instant addedAt
) {
}

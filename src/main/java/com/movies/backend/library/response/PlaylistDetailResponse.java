package com.movies.backend.library.response;

import com.movies.backend.user.response.UserMiniResponse;
import java.util.List;

/** Playlist aberta: o resumo + as faixas em ordem + quem colabora. */
public record PlaylistDetailResponse(
        PlaylistSummaryResponse playlist,
        List<PlaylistItemResponse> items,
        List<UserMiniResponse> collaborators
) {
}

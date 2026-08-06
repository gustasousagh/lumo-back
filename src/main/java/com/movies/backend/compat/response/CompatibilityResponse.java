package com.movies.backend.compat.response;

import java.util.List;

/** Compatibilidade de gosto entre o usuário logado e outro perfil. */
public record CompatibilityResponse(
        int score,            // 0..100
        int sharedCount,
        String label,
        List<SharedTitle> sharedTitles
) {
    public record SharedTitle(String mediaType, Long mediaId, String title, String posterUrl) {
    }
}

package com.movies.backend.library.response;

/** Álbum agregado a partir das faixas (não existe tabela de álbum). */
public record AlbumResponse(
        String name,
        String artist,
        String coverUrl,
        long trackCount,
        Integer year
) {
}

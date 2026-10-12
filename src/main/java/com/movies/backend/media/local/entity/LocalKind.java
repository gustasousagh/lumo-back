package com.movies.backend.media.local.entity;

import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;

/** Os três catálogos do GoCine. Série e anime têm ids próprios, que colidem entre si. */
public enum LocalKind {
    MOVIE,
    SERIES,
    ANIME;

    public MediaType mediaType() {
        return this == MOVIE ? MediaType.MOVIE : MediaType.TV;
    }

    public ContentKind contentKind() {
        return switch (this) {
            case MOVIE -> null;
            case SERIES -> ContentKind.SERIES;
            case ANIME -> ContentKind.ANIME;
        };
    }

    /** Do par que o front manda (type + kind). TV sem kind é série. */
    public static LocalKind of(MediaType type, ContentKind kind) {
        if (type == MediaType.MOVIE) return MOVIE;
        return kind == ContentKind.ANIME ? ANIME : SERIES;
    }
}

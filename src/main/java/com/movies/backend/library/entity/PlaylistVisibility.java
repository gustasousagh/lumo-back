package com.movies.backend.library.entity;

/** Quem enxerga uma playlist. */
public enum PlaylistVisibility {
    /** Só o dono e os colaboradores convidados. */
    PRIVATE,
    /** Quem tiver o link (código de compartilhamento) abre; não aparece em listagens. */
    UNLISTED,
    /** Aparece na aba "Da galera" para todo mundo. */
    PUBLIC
}

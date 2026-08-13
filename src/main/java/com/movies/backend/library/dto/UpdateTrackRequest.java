package com.movies.backend.library.dto;

import jakarta.validation.constraints.Size;

/**
 * Correção manual dos metadados de uma faixa (painel admin). Todo campo é
 * opcional: o que vier nulo fica como está. Para limpar um campo, mande string
 * vazia.
 */
public record UpdateTrackRequest(
        @Size(max = 300) String title,
        @Size(max = 200) String artist,
        @Size(max = 200) String album,
        @Size(max = 200) String albumArtist,
        @Size(max = 100) String genre,
        Integer year,
        Integer trackNumber,
        Integer discNumber,
        String coverUrl
) {
}

package com.movies.backend.library.dto;

import jakarta.validation.constraints.Size;

/** Atualização parcial: o que vier nulo não muda. */
public record UpdatePlaylistRequest(
        @Size(max = 120) String title,
        @Size(max = 500) String description,
        String coverUrl,
        String visibility,
        Boolean collaborative
) {
}

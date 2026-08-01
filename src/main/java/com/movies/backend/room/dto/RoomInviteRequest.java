package com.movies.backend.room.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo do POST /api/rooms/{id}/invite. */
public record RoomInviteRequest(
        @NotBlank(message = "Informe o usuário a convidar")
        String username
) {
}

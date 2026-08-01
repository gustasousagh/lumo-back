package com.movies.backend.friend.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo do POST /api/friends/requests. */
public record SendFriendRequest(
        @NotBlank(message = "Informe o usuário de destino")
        String toUsername
) {
}

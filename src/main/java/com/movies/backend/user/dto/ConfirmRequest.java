package com.movies.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmRequest(
        @NotBlank(message = "Token ausente")
        String token
) {
}

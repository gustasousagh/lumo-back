package com.movies.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendConfirmationRequest(
        @NotBlank(message = "Informe seu email")
        @Email(message = "Email inválido")
        String email
) {
}

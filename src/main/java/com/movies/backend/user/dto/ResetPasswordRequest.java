package com.movies.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Token ausente")
        String token,

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 6, max = 100, message = "A senha deve ter ao menos 6 caracteres")
        String newPassword
) {
}

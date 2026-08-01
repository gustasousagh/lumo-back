package com.movies.backend.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Atualização parcial do perfil. Todos os campos são opcionais; só os
 * enviados (não nulos) são aplicados. O username tem regras próprias.
 */
public record UpdateProfileRequest(
        @Size(min = 2, max = 60, message = "Nome deve ter entre 2 e 60 caracteres")
        String name,

        @Size(min = 3, max = 20, message = "Usuário deve ter entre 3 e 20 caracteres")
        @Pattern(regexp = "^[a-z0-9_]+$", message = "Usuário só pode ter letras minúsculas, números e _")
        String username,

        @Size(max = 500, message = "Bio muito longa")
        String bio,

        String avatarUrl,
        String coverUrl,
        String accent
) {
}

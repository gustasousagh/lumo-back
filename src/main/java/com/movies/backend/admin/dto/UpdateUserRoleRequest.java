package com.movies.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** Promove ou rebaixa alguém. Valores aceitos: "user" ou "admin". */
public record UpdateUserRoleRequest(
        @NotBlank(message = "Informe o papel") String role
) {
}

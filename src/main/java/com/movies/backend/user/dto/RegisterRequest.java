package com.movies.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO (Data Transfer Object) = o formato do JSON que CHEGA na API.
 * As anotações validam a entrada antes de o Controller processar.
 */
public record RegisterRequest(
        @NotBlank(message = "Informe seu nome")
        @Size(min = 2, max = 60, message = "Nome deve ter entre 2 e 60 caracteres")
        String name,

        @NotBlank(message = "Informe seu email")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Informe uma senha")
        @Size(min = 6, max = 100, message = "A senha deve ter ao menos 6 caracteres")
        String password
) {
}

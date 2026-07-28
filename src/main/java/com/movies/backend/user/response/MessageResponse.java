package com.movies.backend.user.response;

/** Resposta simples com uma mensagem (ex.: "Confira seu email"). */
public record MessageResponse(
        String message
) {
}

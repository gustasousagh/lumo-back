package com.movies.backend.user.response;

/** Resposta do login: o token JWT + os dados do usuário logado. */
public record AuthResponse(
        String token,
        UserResponse user
) {
}

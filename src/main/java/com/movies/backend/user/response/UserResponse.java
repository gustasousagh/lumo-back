package com.movies.backend.user.response;

import com.movies.backend.user.entity.User;

/**
 * Response = o formato do JSON que SAI da API. Repare que NÃO expomos a senha.
 * O Service converte a Entity -> Response antes de devolver ao Controller.
 */
public record UserResponse(
        Long id,
        String name,
        String email
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}

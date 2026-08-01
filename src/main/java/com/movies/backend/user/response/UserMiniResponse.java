package com.movies.backend.user.response;

import com.movies.backend.user.entity.User;

/** Mini perfil usado em listas (busca, amigos, participantes, autores). */
public record UserMiniResponse(
        Long id,
        String name,
        String username,
        String avatarUrl,
        String accent
) {
    public static UserMiniResponse from(User user) {
        return new UserMiniResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getAccent());
    }
}

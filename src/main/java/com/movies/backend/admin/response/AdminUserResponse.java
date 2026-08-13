package com.movies.backend.admin.response;

import com.movies.backend.user.entity.User;
import java.time.Instant;

/**
 * Usuário como o painel admin vê: inclui email, papel e situação da conta —
 * campos que o {@code ProfileResponse} público não expõe.
 */
public record AdminUserResponse(
        Long id,
        String name,
        String username,
        String email,
        String avatarUrl,
        String accent,
        String role,
        boolean enabled,
        boolean suspended,
        boolean online,
        Instant createdAt
) {
    public static AdminUserResponse from(User user, boolean online) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getAccent(),
                user.role().dbValue(),
                user.isEnabled(),
                user.isSuspended(),
                online,
                user.getCreatedAt());
    }
}

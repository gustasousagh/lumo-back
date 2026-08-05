package com.movies.backend.notification.response;

import com.movies.backend.notification.entity.Notification;
import com.movies.backend.user.response.UserMiniResponse;
import java.time.Instant;

/** Formato JSON de uma notificação enviada ao cliente. */
public record NotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        String link,
        boolean read,
        Long actorId,
        UserMiniResponse actor,
        Instant createdAt
) {
    /** Monta a partir da notificação, embutindo o mini perfil do ator (pode ser null). */
    public static NotificationResponse from(Notification n, UserMiniResponse actor) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getBody(),
                n.getLink(),
                n.isRead(),
                n.getActorId(),
                actor,
                n.getCreatedAt());
    }
}

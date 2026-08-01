package com.movies.backend.notification.response;

import com.movies.backend.notification.entity.Notification;
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
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getBody(),
                n.getLink(),
                n.isRead(),
                n.getActorId(),
                n.getCreatedAt());
    }
}

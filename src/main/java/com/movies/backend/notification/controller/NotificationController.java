package com.movies.backend.notification.controller;

import com.movies.backend.notification.response.NotificationResponse;
import com.movies.backend.notification.service.NotificationService;
import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.response.MessageResponse;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints das notificações do usuário logado. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notificationService, CurrentUser currentUser) {
        this.notificationService = notificationService;
        this.currentUser = currentUser;
    }

    /** GET /api/notifications -> últimas notificações (mais novas primeiro). */
    @GetMapping
    public List<NotificationResponse> list(Authentication auth) {
        User me = currentUser.require(auth);
        return notificationService.list(me);
    }

    /** GET /api/notifications/unread-count -> {count}. */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        User me = currentUser.require(auth);
        return Map.of("count", notificationService.unreadCount(me));
    }

    /** POST /api/notifications/{id}/read -> marca uma como lida. */
    @PostMapping("/{id}/read")
    public MessageResponse markRead(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        notificationService.markRead(me, id);
        return new MessageResponse("Notificação marcada como lida");
    }

    /** POST /api/notifications/read-all -> marca todas como lidas. */
    @PostMapping("/read-all")
    public MessageResponse markAllRead(Authentication auth) {
        User me = currentUser.require(auth);
        notificationService.markAllRead(me);
        return new MessageResponse("Todas as notificações foram marcadas como lidas");
    }
}

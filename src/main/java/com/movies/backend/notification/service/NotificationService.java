package com.movies.backend.notification.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.notification.entity.Notification;
import com.movies.backend.notification.entity.NotificationType;
import com.movies.backend.notification.repository.NotificationRepository;
import com.movies.backend.notification.response.NotificationResponse;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import com.movies.backend.user.response.UserMiniResponse;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras das notificações: salva no banco e, se o WebSocket estiver disponível,
 * empurra em tempo real para a fila pessoal do usuário
 * (/user/{email}/queue/notifications).
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               ObjectProvider<SimpMessagingTemplate> messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /** Cria a notificação, salva e tenta enviar em tempo real. */
    @Transactional
    public Notification push(Long userId, NotificationType type, String title,
                             String body, String link, Long actorId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLink(link);
        notification.setActorId(actorId);
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);

        // Push em tempo real (best-effort). Precisa do email para o destino pessoal.
        SimpMessagingTemplate template = messagingTemplate.getIfAvailable();
        if (template != null) {
            userRepository.findById(userId).ifPresent(user ->
                    template.convertAndSendToUser(
                            user.getEmail(),
                            "/queue/notifications",
                            toResponse(saved)));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(User me) {
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(me.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Monta a resposta embutindo o mini perfil do ator (resolvido por actorId). */
    private NotificationResponse toResponse(Notification n) {
        UserMiniResponse actor = null;
        if (n.getActorId() != null) {
            actor = userRepository.findById(n.getActorId())
                    .map(UserMiniResponse::from)
                    .orElse(null);
        }
        return NotificationResponse.from(n, actor);
    }

    @Transactional(readOnly = true)
    public long unreadCount(User me) {
        return notificationRepository.countByUserIdAndReadFalse(me.getId());
    }

    @Transactional
    public void markRead(User me, Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Notificação não encontrada"));
        if (!notification.getUserId().equals(me.getId())) {
            throw ApiException.forbidden("Essa notificação não é sua");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(User me) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(me.getId());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}

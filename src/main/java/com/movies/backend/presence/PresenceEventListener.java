package com.movies.backend.presence;

import com.movies.backend.presence.service.PresenceService;
import com.movies.backend.user.repository.UserRepository;
import java.security.Principal;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Ouve os eventos de conexão/desconexão STOMP e atualiza a presença.
 * O Principal setado no CONNECT (ver WebSocketConfig) tem name == email do usuário.
 */
@Component
public class PresenceEventListener {

    private final PresenceService presenceService;
    private final UserRepository userRepository;

    public PresenceEventListener(PresenceService presenceService, UserRepository userRepository) {
        this.presenceService = presenceService;
        this.userRepository = userRepository;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        resolveUserId(StompHeaderAccessor.wrap(event.getMessage()).getUser())
                .ifPresent(presenceService::connect);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        resolveUserId(StompHeaderAccessor.wrap(event.getMessage()).getUser())
                .ifPresent(presenceService::disconnect);
    }

    /** Do Principal (name == email) resolve o id do usuário. */
    private java.util.Optional<Long> resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return java.util.Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(principal.getName())
                .map(u -> u.getId());
    }
}

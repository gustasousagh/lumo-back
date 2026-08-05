package com.movies.backend.presence.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Presença online/offline em memória. Conta quantas sessões WebSocket ativas
 * cada usuário tem; quando chega a zero, o usuário é considerado offline.
 * Simples e volátil de propósito (some quando o servidor reinicia).
 */
@Service
public class PresenceService {

    /** userId -> quantidade de sessões WS ativas. */
    private final ConcurrentHashMap<Long, Integer> sessions = new ConcurrentHashMap<>();

    /** Registra uma nova sessão (conexão) para o usuário. */
    public void connect(Long userId) {
        if (userId == null) {
            return;
        }
        sessions.merge(userId, 1, Integer::sum);
    }

    /** Remove uma sessão (desconexão); zera -> offline. */
    public void disconnect(Long userId) {
        if (userId == null) {
            return;
        }
        sessions.computeIfPresent(userId, (id, count) -> count <= 1 ? null : count - 1);
    }

    /** true se o usuário tem ao menos uma sessão ativa. */
    public boolean isOnline(Long userId) {
        return userId != null && sessions.containsKey(userId);
    }

    /** Conjunto de ids de usuários online no momento. */
    public Set<Long> onlineUserIds() {
        return Set.copyOf(sessions.keySet());
    }
}

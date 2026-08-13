package com.movies.backend.user.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.friend.response.WatchingNowResponse;
import com.movies.backend.friend.service.FriendService;
import com.movies.backend.presence.service.PresenceService;
import com.movies.backend.user.dto.UpdateWatchingRequest;
import com.movies.backend.user.dto.UpdateProfileRequest;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import com.movies.backend.user.response.ProfileResponse;
import com.movies.backend.user.response.UserMiniResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras do perfil: montar o perfil próprio/alheio, atualizar dados e buscar
 * usuários. Depende do FriendService para status e contagem de amizades.
 */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final FriendService friendService;
    private final PresenceService presenceService;

    public ProfileService(UserRepository userRepository,
                          FriendService friendService,
                          PresenceService presenceService) {
        this.userRepository = userRepository;
        this.friendService = friendService;
        this.presenceService = presenceService;
    }

    /** Perfil completo do próprio usuário (inclui email). */
    @Transactional(readOnly = true)
    public ProfileResponse me(User me) {
        return build(me, me, true);
    }

    /** Perfil público de outra pessoa pelo username, com status relativo a "me". */
    @Transactional(readOnly = true)
    public ProfileResponse getByUsername(User me, String username) {
        User target = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));
        boolean self = target.getId().equals(me.getId());
        return build(target, me, self);
    }

    /** Atualização parcial do perfil próprio. */
    @Transactional
    public ProfileResponse update(User me, UpdateProfileRequest req) {
        if (req.name() != null && !req.name().isBlank()) {
            me.setName(req.name().trim());
        }
        if (req.username() != null) {
            String username = req.username().trim().toLowerCase();
            userRepository.findByUsername(username).ifPresent(other -> {
                if (!other.getId().equals(me.getId())) {
                    throw ApiException.conflict("Esse nome de usuário já está em uso");
                }
            });
            me.setUsername(username);
        }
        if (req.bio() != null) {
            me.setBio(req.bio());
        }
        if (req.avatarUrl() != null) {
            me.setAvatarUrl(req.avatarUrl());
        }
        if (req.coverUrl() != null) {
            me.setCoverUrl(req.coverUrl());
        }
        if (req.accent() != null && !req.accent().isBlank()) {
            me.setAccent(req.accent());
        }
        User saved = userRepository.save(me);
        return build(saved, saved, true);
    }

    /** Busca usuários por username ou nome (mínimo 2 caracteres). */
    @Transactional(readOnly = true)
    public List<UserMiniResponse> search(String q) {
        if (q == null || q.trim().length() < 2) {
            throw ApiException.badRequest("Digite ao menos 2 caracteres");
        }
        String term = q.trim();
        return userRepository
                .findTop20ByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(term, term)
                .stream()
                .map(UserMiniResponse::from)
                .toList();
    }

    // -------------------------------------------------------------- HELPERS
    private ProfileResponse build(User target, User viewer, boolean self) {
        String friendStatus = friendService.friendStatus(viewer, target);
        return new ProfileResponse(
                target.getId(),
                target.getName(),
                target.getUsername(),
                self ? target.getEmail() : null,
                target.getBio(),
                target.getAvatarUrl(),
                target.getCoverUrl(),
                target.getAccent(),
                target.role().dbValue(),
                target.getCreatedAt(),
                friendService.friendCount(target),
                friendStatus,
                presenceService.isOnline(target.getId()),
                self ? null : WatchingNowResponse.from(target));
    }

    // ------------------------------------------------------ "ASSISTINDO AGORA"
    /** Define/atualiza o que o usuário está assistindo agora. */
    @Transactional
    public void updateWatching(User me, UpdateWatchingRequest req) {
        me.setWatchingMediaType(req.mediaType());
        me.setWatchingMediaId(req.mediaId());
        me.setWatchingTitle(req.title());
        me.setWatchingPosterUrl(req.posterUrl());
        me.setWatchingUpdatedAt(Instant.now());
        userRepository.save(me);
    }

    /** Limpa o "assistindo agora" do usuário. */
    @Transactional
    public void clearWatching(User me) {
        me.setWatchingMediaType(null);
        me.setWatchingMediaId(null);
        me.setWatchingTitle(null);
        me.setWatchingPosterUrl(null);
        me.setWatchingUpdatedAt(null);
        userRepository.save(me);
    }
}

package com.movies.backend.library.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.library.dto.CreatePlaylistRequest;
import com.movies.backend.library.dto.UpdatePlaylistRequest;
import com.movies.backend.library.entity.Playlist;
import com.movies.backend.library.entity.PlaylistCollaborator;
import com.movies.backend.library.entity.PlaylistTrack;
import com.movies.backend.library.entity.PlaylistVisibility;
import com.movies.backend.library.entity.Track;
import com.movies.backend.library.repository.PlaylistCollaboratorRepository;
import com.movies.backend.library.repository.PlaylistRepository;
import com.movies.backend.library.repository.PlaylistTrackRepository;
import com.movies.backend.library.repository.TrackRepository;
import com.movies.backend.library.response.PlaylistDetailResponse;
import com.movies.backend.library.response.PlaylistItemResponse;
import com.movies.backend.library.response.PlaylistSummaryResponse;
import com.movies.backend.library.response.TrackResponse;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import com.movies.backend.user.response.UserMiniResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Playlists de música: criar, montar, reordenar, compartilhar e colaborar.
 *
 * <p>Regras de permissão em um lugar só ({@link #canView} e {@link #canEdit}):
 * <ul>
 *   <li>ver — o dono sempre; colaborador sempre; qualquer pessoa se for PUBLIC;
 *       quem chega pelo código de compartilhamento se for UNLISTED ou PUBLIC.</li>
 *   <li>editar faixas — o dono, e os colaboradores quando a playlist é
 *       colaborativa.</li>
 *   <li>renomear, mudar visibilidade e apagar — só o dono. Colaborador entrou
 *       para montar o repertório, não para dissolver a playlist.</li>
 * </ul>
 */
@Service
public class PlaylistService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "abcdefghijkmnopqrstuvwxyz23456789";
    private static final int CODE_LENGTH = 8;

    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository itemRepository;
    private final PlaylistCollaboratorRepository collaboratorRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;

    public PlaylistService(PlaylistRepository playlistRepository,
                           PlaylistTrackRepository itemRepository,
                           PlaylistCollaboratorRepository collaboratorRepository,
                           TrackRepository trackRepository,
                           UserRepository userRepository) {
        this.playlistRepository = playlistRepository;
        this.itemRepository = itemRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.trackRepository = trackRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ CRIAR
    @Transactional
    public PlaylistSummaryResponse create(User me, CreatePlaylistRequest req) {
        Playlist playlist = new Playlist();
        playlist.setOwnerId(me.getId());
        playlist.setTitle(req.title().trim());
        playlist.setDescription(blankToNull(req.description()));
        playlist.setVisibility(parseVisibility(req.visibility(), PlaylistVisibility.PRIVATE));
        playlist.setCollaborative(req.collaborative());
        playlist.setShareCode(newShareCode());
        return summary(me, playlistRepository.save(playlist));
    }

    @Transactional
    public PlaylistSummaryResponse update(User me, Long id, UpdatePlaylistRequest req) {
        Playlist playlist = requireOwner(me, id);
        if (req.title() != null && !req.title().isBlank()) {
            playlist.setTitle(req.title().trim());
        }
        if (req.description() != null) {
            playlist.setDescription(blankToNull(req.description()));
        }
        if (req.coverUrl() != null) {
            playlist.setCoverUrl(blankToNull(req.coverUrl()));
        }
        if (req.visibility() != null) {
            playlist.setVisibility(parseVisibility(req.visibility(), playlist.getVisibility()));
        }
        if (req.collaborative() != null) {
            playlist.setCollaborative(req.collaborative());
        }
        playlist.setUpdatedAt(Instant.now());
        return summary(me, playlistRepository.save(playlist));
    }

    @Transactional
    public void delete(User me, Long id) {
        Playlist playlist = requireOwner(me, id);
        itemRepository.deleteByPlaylistId(id);
        collaboratorRepository.deleteByPlaylistId(id);
        playlistRepository.delete(playlist);
    }

    /** Gera um código novo — invalida o link antigo que já circulou por aí. */
    @Transactional
    public PlaylistSummaryResponse rotateShareCode(User me, Long id) {
        Playlist playlist = requireOwner(me, id);
        playlist.setShareCode(newShareCode());
        playlist.setUpdatedAt(Instant.now());
        return summary(me, playlistRepository.save(playlist));
    }

    // --------------------------------------------------------------- LISTAGEM
    @Transactional(readOnly = true)
    public List<PlaylistSummaryResponse> myPlaylists(User me) {
        return playlistRepository.findByOwnerIdOrderByUpdatedAtDesc(me.getId())
                .stream().map(p -> summary(me, p)).toList();
    }

    /** Playlists de outras pessoas onde eu fui convidado como colaborador. */
    @Transactional(readOnly = true)
    public List<PlaylistSummaryResponse> sharedWithMe(User me) {
        List<Long> ids = collaboratorRepository.findByUserId(me.getId())
                .stream().map(PlaylistCollaborator::getPlaylistId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return playlistRepository.findByIdInOrderByUpdatedAtDesc(ids)
                .stream().map(p -> summary(me, p)).toList();
    }

    /** Playlists públicas — a aba "Da galera". Exclui as minhas, que já aparecem. */
    @Transactional(readOnly = true)
    public List<PlaylistSummaryResponse> publicPlaylists(User me) {
        return playlistRepository.findTop50ByVisibilityOrderByUpdatedAtDesc(PlaylistVisibility.PUBLIC)
                .stream()
                .filter(p -> !p.getOwnerId().equals(me.getId()))
                .map(p -> summary(me, p))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaylistSummaryResponse> searchVisible(User me, String lowerTerm) {
        Set<Playlist> candidates = new LinkedHashSet<>();
        candidates.addAll(playlistRepository.findByOwnerIdOrderByUpdatedAtDesc(me.getId()));
        candidates.addAll(playlistRepository.findTop50ByVisibilityOrderByUpdatedAtDesc(PlaylistVisibility.PUBLIC));
        return candidates.stream()
                .filter(p -> p.getTitle().toLowerCase().contains(lowerTerm))
                .limit(12)
                .map(p -> summary(me, p))
                .toList();
    }

    public long totalCount() {
        return playlistRepository.count();
    }

    // ---------------------------------------------------------------- DETALHE
    @Transactional(readOnly = true)
    public PlaylistDetailResponse detail(User me, Long id) {
        Playlist playlist = require(id);
        if (!canView(me, playlist, false)) {
            throw ApiException.forbidden("Essa playlist é privada");
        }
        return detailOf(me, playlist);
    }

    /** Abre pelo link compartilhado. O código já é a autorização de leitura. */
    @Transactional(readOnly = true)
    public PlaylistDetailResponse byShareCode(User me, String code) {
        Playlist playlist = playlistRepository.findByShareCode(code)
                .orElseThrow(() -> ApiException.notFound("Link inválido ou expirado"));
        if (!canView(me, playlist, true)) {
            throw ApiException.forbidden("Essa playlist não está mais compartilhada");
        }
        return detailOf(me, playlist);
    }

    private PlaylistDetailResponse detailOf(User me, Playlist playlist) {
        List<PlaylistTrack> items = itemRepository.findByPlaylistIdOrderByPositionAsc(playlist.getId());
        Map<Long, Track> tracksById = tracksOf(items);
        Map<Long, UserMiniResponse> usersById = usersOf(items.stream()
                .map(PlaylistTrack::getAddedById).collect(Collectors.toSet()));

        List<PlaylistItemResponse> resolved = new ArrayList<>(items.size());
        for (PlaylistTrack item : items) {
            Track track = tracksById.get(item.getTrackId());
            if (track == null) {
                continue; // faixa apagada por um admin: some da lista
            }
            resolved.add(new PlaylistItemResponse(
                    item.getId(),
                    item.getPosition(),
                    TrackResponse.from(track),
                    usersById.get(item.getAddedById()),
                    item.getAddedAt()));
        }

        List<UserMiniResponse> collaborators = collaboratorRepository.findByPlaylistId(playlist.getId())
                .stream()
                .map(c -> userRepository.findById(c.getUserId()).orElse(null))
                .filter(Objects::nonNull)
                .map(UserMiniResponse::from)
                .toList();

        return new PlaylistDetailResponse(summary(me, playlist), resolved, collaborators);
    }

    // ------------------------------------------------------------------ ITENS
    /** Acrescenta faixas no fim, na ordem em que vieram. */
    @Transactional
    public PlaylistDetailResponse addTracks(User me, Long playlistId, List<Long> trackIds) {
        Playlist playlist = requireEditor(me, playlistId);
        int position = itemRepository.maxPosition(playlistId) + 1;
        for (Long trackId : trackIds) {
            if (trackRepository.findById(trackId).isEmpty()) {
                continue;
            }
            PlaylistTrack item = new PlaylistTrack();
            item.setPlaylistId(playlistId);
            item.setTrackId(trackId);
            item.setAddedById(me.getId());
            item.setPosition(position++);
            itemRepository.save(item);
        }
        touch(playlist);
        return detailOf(me, playlist);
    }

    @Transactional
    public PlaylistDetailResponse removeItem(User me, Long playlistId, Long itemId) {
        Playlist playlist = requireEditor(me, playlistId);
        PlaylistTrack item = itemRepository.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Item não encontrado"));
        if (!item.getPlaylistId().equals(playlistId)) {
            throw ApiException.badRequest("Esse item não é dessa playlist");
        }
        itemRepository.delete(item);
        normalizePositions(playlistId);
        touch(playlist);
        return detailOf(me, playlist);
    }

    /**
     * Aplica a nova ordem. Ids que não são da playlist são ignorados, e itens
     * que a pessoa não mandou (porque alguém adicionou enquanto ela arrastava)
     * vão para o fim, preservando a ordem relativa que tinham.
     */
    @Transactional
    public PlaylistDetailResponse reorder(User me, Long playlistId, List<Long> itemIds) {
        Playlist playlist = requireEditor(me, playlistId);
        List<PlaylistTrack> current = itemRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        Map<Long, PlaylistTrack> byId = current.stream()
                .collect(Collectors.toMap(PlaylistTrack::getId, Function.identity()));

        int position = 0;
        Set<Long> placed = new LinkedHashSet<>();
        for (Long itemId : itemIds) {
            PlaylistTrack item = byId.get(itemId);
            if (item == null || !placed.add(itemId)) {
                continue;
            }
            item.setPosition(position++);
            itemRepository.save(item);
        }
        for (PlaylistTrack item : current) {
            if (!placed.contains(item.getId())) {
                item.setPosition(position++);
                itemRepository.save(item);
            }
        }
        touch(playlist);
        return detailOf(me, playlist);
    }

    // --------------------------------------------------------- COLABORADORES
    @Transactional
    public PlaylistDetailResponse addCollaborator(User me, Long playlistId, String username) {
        Playlist playlist = requireOwner(me, playlistId);
        User target = userRepository.findByUsername(username.trim().toLowerCase().replace("@", ""))
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));
        if (target.getId().equals(playlist.getOwnerId())) {
            throw ApiException.badRequest("Você já é o dono dessa playlist");
        }
        if (!collaboratorRepository.existsByPlaylistIdAndUserId(playlistId, target.getId())) {
            PlaylistCollaborator collaborator = new PlaylistCollaborator();
            collaborator.setPlaylistId(playlistId);
            collaborator.setUserId(target.getId());
            collaboratorRepository.save(collaborator);
        }
        // Convidar alguém sem deixar colaborativa não faria sentido nenhum.
        if (!playlist.isCollaborative()) {
            playlist.setCollaborative(true);
        }
        touch(playlist);
        return detailOf(me, playlist);
    }

    /** O dono remove qualquer um; um colaborador pode remover a si mesmo (sair). */
    @Transactional
    public PlaylistDetailResponse removeCollaborator(User me, Long playlistId, Long userId) {
        Playlist playlist = require(playlistId);
        boolean owner = playlist.getOwnerId().equals(me.getId());
        if (!owner && !me.getId().equals(userId)) {
            throw ApiException.forbidden("Só o dono pode remover colaboradores");
        }
        collaboratorRepository.findByPlaylistIdAndUserId(playlistId, userId)
                .ifPresent(collaboratorRepository::delete);
        touch(playlist);
        return detailOf(me, playlist);
    }

    // ------------------------------------------------------------- PERMISSÕES
    public boolean canView(User me, Playlist playlist, boolean viaShareLink) {
        if (me != null && playlist.getOwnerId().equals(me.getId())) {
            return true;
        }
        if (playlist.getVisibility() == PlaylistVisibility.PUBLIC) {
            return true;
        }
        if (viaShareLink && playlist.getVisibility() == PlaylistVisibility.UNLISTED) {
            return true;
        }
        return me != null && collaboratorRepository.existsByPlaylistIdAndUserId(playlist.getId(), me.getId());
    }

    public boolean canEdit(User me, Playlist playlist) {
        if (me == null) {
            return false;
        }
        if (playlist.getOwnerId().equals(me.getId())) {
            return true;
        }
        return playlist.isCollaborative()
                && collaboratorRepository.existsByPlaylistIdAndUserId(playlist.getId(), me.getId());
    }

    public Playlist require(Long id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Playlist não encontrada"));
    }

    private Playlist requireOwner(User me, Long id) {
        Playlist playlist = require(id);
        if (!playlist.getOwnerId().equals(me.getId())) {
            throw ApiException.forbidden("Essa playlist não é sua");
        }
        return playlist;
    }

    private Playlist requireEditor(User me, Long id) {
        Playlist playlist = require(id);
        if (!canEdit(me, playlist)) {
            throw ApiException.forbidden("Você não pode editar essa playlist");
        }
        return playlist;
    }

    // ---------------------------------------------------------------- HELPERS
    /** Resumo com contagens, capas e as permissões de quem está olhando. */
    public PlaylistSummaryResponse summary(User me, Playlist playlist) {
        List<PlaylistTrack> items = itemRepository.findByPlaylistIdOrderByPositionAsc(playlist.getId());
        Map<Long, Track> tracksById = tracksOf(items);

        List<String> covers = new ArrayList<>();
        int duration = 0;
        for (PlaylistTrack item : items) {
            Track track = tracksById.get(item.getTrackId());
            if (track == null) {
                continue;
            }
            if (track.getDurationSec() != null) {
                duration += track.getDurationSec();
            }
            if (covers.size() < 4 && track.getCoverUrl() != null && !covers.contains(track.getCoverUrl())) {
                covers.add(track.getCoverUrl());
            }
        }

        User owner = userRepository.findById(playlist.getOwnerId()).orElse(null);
        return new PlaylistSummaryResponse(
                playlist.getId(),
                playlist.getTitle(),
                playlist.getDescription(),
                playlist.getCoverUrl(),
                covers,
                playlist.getVisibility().name(),
                playlist.getShareCode(),
                playlist.isCollaborative(),
                owner == null ? null : UserMiniResponse.from(owner),
                items.size(),
                duration,
                collaboratorRepository.findByPlaylistId(playlist.getId()).size(),
                canEdit(me, playlist),
                me != null && playlist.getOwnerId().equals(me.getId()),
                playlist.getUpdatedAt());
    }

    private Map<Long, Track> tracksOf(List<PlaylistTrack> items) {
        if (items.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = items.stream().map(PlaylistTrack::getTrackId).distinct().toList();
        Map<Long, Track> map = new HashMap<>();
        for (Track track : trackRepository.findByIdIn(ids)) {
            map.put(track.getId(), track);
        }
        return map;
    }

    private Map<Long, UserMiniResponse> usersOf(Set<Long> ids) {
        Map<Long, UserMiniResponse> map = new HashMap<>();
        for (Long id : ids) {
            userRepository.findById(id).ifPresent(user -> map.put(id, UserMiniResponse.from(user)));
        }
        return map;
    }

    /** Reescreve as posições em 0..n-1 depois de uma remoção. */
    private void normalizePositions(Long playlistId) {
        int position = 0;
        for (PlaylistTrack item : itemRepository.findByPlaylistIdOrderByPositionAsc(playlistId)) {
            if (item.getPosition() != position) {
                item.setPosition(position);
                itemRepository.save(item);
            }
            position++;
        }
    }

    private void touch(Playlist playlist) {
        playlist.setUpdatedAt(Instant.now());
        playlistRepository.save(playlist);
    }

    private PlaylistVisibility parseVisibility(String raw, PlaylistVisibility fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return PlaylistVisibility.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Visibilidade inválida (use PRIVATE, UNLISTED ou PUBLIC)");
        }
    }

    /**
     * Código curto e legível. O alfabeto não tem "l", "1", "0" nem "o": o link
     * costuma ser ditado ou digitado à mão, e esses são os pares que as pessoas
     * confundem.
     */
    private String newShareCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = builder.toString();
            if (!playlistRepository.existsByShareCode(code)) {
                return code;
            }
        }
        throw ApiException.conflict("Não foi possível gerar o link agora, tente de novo");
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

package com.movies.backend.room.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.media.entity.MediaCatalog;
import com.movies.backend.media.repository.MediaCatalogRepository;
import com.movies.backend.media.response.MediaCatalogResponse;
import com.movies.backend.media.service.MediaCatalogService;
import com.movies.backend.notification.entity.NotificationType;
import com.movies.backend.notification.service.NotificationService;
import com.movies.backend.room.dto.CreateRoomRequest;
import com.movies.backend.room.entity.Room;
import com.movies.backend.room.entity.RoomInvite;
import com.movies.backend.room.entity.RoomInviteStatus;
import com.movies.backend.room.entity.RoomParticipant;
import com.movies.backend.room.entity.RoomRole;
import com.movies.backend.room.entity.RoomStatus;
import com.movies.backend.room.repository.RoomInviteRepository;
import com.movies.backend.room.repository.RoomParticipantRepository;
import com.movies.backend.room.repository.RoomRepository;
import com.movies.backend.room.response.ParticipantResponse;
import com.movies.backend.room.response.RoomResponse;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import com.movies.backend.user.response.UserMiniResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras das salas de watch party (baseadas em convites). */
@Service
public class RoomService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final RoomInviteRepository inviteRepository;
    private final MediaCatalogService catalogService;
    private final MediaCatalogRepository catalogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public RoomService(RoomRepository roomRepository,
                       RoomParticipantRepository participantRepository,
                       RoomInviteRepository inviteRepository,
                       MediaCatalogService catalogService,
                       MediaCatalogRepository catalogRepository,
                       UserRepository userRepository,
                       NotificationService notificationService) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.inviteRepository = inviteRepository;
        this.catalogService = catalogService;
        this.catalogRepository = catalogRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // ---------------------------------------------------------------- CRIAR
    @Transactional
    public RoomResponse create(User me, CreateRoomRequest req) {
        MediaCatalog catalog = catalogService.upsert(req.toSnapshot());

        Room room = new Room();
        room.setCode(generateUniqueCode());
        room.setOwnerId(me.getId());
        room.setCatalogId(catalog.getId());
        room.setStatus(RoomStatus.ACTIVE);
        room.setPlaying(false);
        room.setCurrentTimeSec(0.0);
        room.setSeasonNumber(req.seasonNumber());
        room.setEpisodeNumber(req.episodeNumber());
        Room saved = roomRepository.save(room);

        RoomParticipant host = new RoomParticipant();
        host.setRoomId(saved.getId());
        host.setUserId(me.getId());
        host.setRole(RoomRole.HOST);
        participantRepository.save(host);

        return toResponse(saved, me);
    }

    // ------------------------------------------------------------------ GET
    @Transactional(readOnly = true)
    public RoomResponse get(User me, Long roomId) {
        Room room = requireRoom(roomId);
        return toResponse(room, me);
    }

    // --------------------------------------------------------------- CONVITE
    @Transactional
    public void invite(User me, Long roomId, String username) {
        Room room = requireRoom(roomId);
        // apenas participantes podem convidar
        participantRepository.findByRoomIdAndUserId(roomId, me.getId())
                .orElseThrow(() -> ApiException.forbidden("Você não está nessa sala"));

        User invitee = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));
        if (invitee.getId().equals(me.getId())) {
            throw ApiException.badRequest("Você já está na sala");
        }

        // evita convite duplicado pendente
        if (inviteRepository.findByRoomIdAndInviteeIdAndStatus(
                roomId, invitee.getId(), RoomInviteStatus.PENDING).isEmpty()) {
            RoomInvite invite = new RoomInvite();
            invite.setRoomId(roomId);
            invite.setInviterId(me.getId());
            invite.setInviteeId(invitee.getId());
            invite.setStatus(RoomInviteStatus.PENDING);
            inviteRepository.save(invite);
        }

        // Notificação (que já empurra via WebSocket para /user/{email}/queue/notifications)
        notificationService.push(invitee.getId(), NotificationType.ROOM_INVITE,
                "Convite para assistir junto",
                me.getName() + " te convidou para uma sala",
                "/rooms/join/" + room.getCode(), me.getId());
    }

    // ----------------------------------------------------------- ENTRAR (code)
    @Transactional
    public RoomResponse joinByCode(User me, String code) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> ApiException.notFound("Sala não encontrada"));
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw ApiException.badRequest("Essa sala já foi encerrada");
        }

        if (participantRepository.findByRoomIdAndUserId(room.getId(), me.getId()).isEmpty()) {
            RoomParticipant participant = new RoomParticipant();
            participant.setRoomId(room.getId());
            participant.setUserId(me.getId());
            participant.setRole(RoomRole.VIEWER);
            participantRepository.save(participant);
        }

        // marca convite pendente como aceito, se houver
        inviteRepository.findByRoomIdAndInviteeIdAndStatus(room.getId(), me.getId(), RoomInviteStatus.PENDING)
                .ifPresent(invite -> {
                    invite.setStatus(RoomInviteStatus.ACCEPTED);
                    inviteRepository.save(invite);
                });

        return toResponse(room, me);
    }

    // -------------------------------------------------------------- SAIR
    @Transactional
    public void leave(User me, Long roomId) {
        Room room = requireRoom(roomId);
        participantRepository.findByRoomIdAndUserId(roomId, me.getId())
                .ifPresent(participantRepository::delete);

        // Se o host saiu, encerra a sala
        if (room.getOwnerId().equals(me.getId())) {
            room.setStatus(RoomStatus.ENDED);
            room.setUpdatedAt(Instant.now());
            roomRepository.save(room);
        }
    }

    // -------------------------------------------------------------- MINHAS
    @Transactional(readOnly = true)
    public List<RoomResponse> mine(User me) {
        Set<Long> roomIds = new LinkedHashSet<>();
        for (RoomParticipant p : participantRepository.findByUserId(me.getId())) {
            roomIds.add(p.getRoomId());
        }
        if (roomIds.isEmpty()) {
            return List.of();
        }
        return roomRepository.findByIdInAndStatus(new ArrayList<>(roomIds), RoomStatus.ACTIVE)
                .stream().map(r -> toResponse(r, me)).toList();
    }

    // ------------------------------------------------- PLAYBACK (via WebSocket)
    /** Persiste o estado de playback vindo do WebSocket. Só o HOST pode. */
    @Transactional
    public Room updatePlayback(Long roomId, String userEmail, boolean isPlaying, double currentTimeSec) {
        Room room = requireRoom(roomId);
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> ApiException.unauthorized("Não autenticado"));
        RoomParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> ApiException.forbidden("Você não está nessa sala"));
        if (participant.getRole() != RoomRole.HOST) {
            throw ApiException.forbidden("Somente o host controla o playback");
        }
        room.setPlaying(isPlaying);
        room.setCurrentTimeSec(currentTimeSec);
        room.setPlaybackUpdatedAt(Instant.now());
        room.setUpdatedAt(Instant.now());
        return roomRepository.save(room);
    }

    /** Troca o episódio da sala (só o HOST). Reseta o playback pro início. */
    @Transactional
    public Room updateEpisode(Long roomId, String userEmail, Integer seasonNumber, Integer episodeNumber) {
        Room room = requireRoom(roomId);
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> ApiException.unauthorized("Não autenticado"));
        RoomParticipant participant = participantRepository.findByRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> ApiException.forbidden("Você não está nessa sala"));
        if (participant.getRole() != RoomRole.HOST) {
            throw ApiException.forbidden("Somente o host troca o episódio");
        }
        room.setSeasonNumber(seasonNumber);
        room.setEpisodeNumber(episodeNumber);
        room.setPlaying(false);
        room.setCurrentTimeSec(0.0);
        room.setPlaybackUpdatedAt(Instant.now());
        room.setUpdatedAt(Instant.now());
        return roomRepository.save(room);
    }

    /** Atualiza o lastSeenAt do participante (presença). */
    @Transactional
    public void touchPresence(Long roomId, String userEmail) {
        userRepository.findByEmailIgnoreCase(userEmail).ifPresent(user ->
                participantRepository.findByRoomIdAndUserId(roomId, user.getId())
                        .ifPresent(p -> {
                            p.setLastSeenAt(Instant.now());
                            participantRepository.save(p);
                        }));
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> participants(Long roomId) {
        List<ParticipantResponse> out = new ArrayList<>();
        for (RoomParticipant p : participantRepository.findByRoomId(roomId)) {
            userRepository.findById(p.getUserId()).ifPresent(u ->
                    out.add(new ParticipantResponse(UserMiniResponse.from(u), p.getRole().name(), p.getJoinedAt())));
        }
        return out;
    }

    // -------------------------------------------------------------- HELPERS
    private Room requireRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> ApiException.notFound("Sala não encontrada"));
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (roomRepository.existsByCode(code));
        return code;
    }

    private RoomResponse toResponse(Room room, User me) {
        MediaCatalogResponse media = null;
        if (room.getCatalogId() != null) {
            media = catalogRepository.findById(room.getCatalogId())
                    .map(MediaCatalogResponse::from)
                    .orElse(null);
        }
        boolean amIHost = room.getOwnerId().equals(me.getId());
        return new RoomResponse(
                room.getId(),
                room.getCode(),
                room.getStatus().name(),
                media,
                room.isPlaying(),
                room.getCurrentTimeSec(),
                room.getPlaybackUpdatedAt(),
                room.getSeasonNumber(),
                room.getEpisodeNumber(),
                participants(room.getId()),
                amIHost,
                room.getCreatedAt());
    }
}

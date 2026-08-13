package com.movies.backend.admin.service;

import com.movies.backend.admin.response.AdminOverviewResponse;
import com.movies.backend.admin.response.AdminUserResponse;
import com.movies.backend.admin.response.PageResponse;
import com.movies.backend.exception.ApiException;
import com.movies.backend.library.entity.PlaylistVisibility;
import com.movies.backend.library.repository.BookRepository;
import com.movies.backend.library.repository.PlayEventRepository;
import com.movies.backend.library.repository.PlaylistRepository;
import com.movies.backend.library.repository.TrackRepository;
import com.movies.backend.library.response.BookResponse;
import com.movies.backend.library.response.TrackResponse;
import com.movies.backend.library.storage.LibraryStorage;
import com.movies.backend.list.repository.MovieListRepository;
import com.movies.backend.post.repository.PostRepository;
import com.movies.backend.presence.service.PresenceService;
import com.movies.backend.room.repository.RoomRepository;
import com.movies.backend.user.entity.Role;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O que o painel admin precisa: números do sistema, séries para os gráficos e
 * gestão de contas.
 *
 * <p>Todo método aqui roda sob {@code hasRole('ADMIN')} — garantido nas duas
 * pontas: pela regra de URL do SecurityConfig e pelo {@code @PreAuthorize} do
 * controller.
 */
@Service
public class AdminService {

    /** Janela dos gráficos, em dias. */
    private static final int WINDOW_DAYS = 30;

    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final BookRepository bookRepository;
    private final PlaylistRepository playlistRepository;
    private final PlayEventRepository playEventRepository;
    private final PostRepository postRepository;
    private final RoomRepository roomRepository;
    private final MovieListRepository listRepository;
    private final PresenceService presenceService;
    private final LibraryStorage storage;

    public AdminService(UserRepository userRepository,
                        TrackRepository trackRepository,
                        BookRepository bookRepository,
                        PlaylistRepository playlistRepository,
                        PlayEventRepository playEventRepository,
                        PostRepository postRepository,
                        RoomRepository roomRepository,
                        MovieListRepository listRepository,
                        PresenceService presenceService,
                        LibraryStorage storage) {
        this.userRepository = userRepository;
        this.trackRepository = trackRepository;
        this.bookRepository = bookRepository;
        this.playlistRepository = playlistRepository;
        this.playEventRepository = playEventRepository;
        this.postRepository = postRepository;
        this.roomRepository = roomRepository;
        this.listRepository = listRepository;
        this.presenceService = presenceService;
        this.storage = storage;
    }

    // --------------------------------------------------------------- VISÃO GERAL
    @Transactional(readOnly = true)
    public AdminOverviewResponse overview() {
        Instant since = Instant.now().minus(WINDOW_DAYS, ChronoUnit.DAYS);

        long musicBytes = trackRepository.totalBytes();
        long booksBytes = bookRepository.totalBytes();

        var totals = new AdminOverviewResponse.Totals(
                userRepository.count(),
                userRepository.countByRole(Role.ADMIN.dbValue()),
                userRepository.countBySuspended(true),
                userRepository.countByEnabled(false),
                userRepository.countByCreatedAtAfter(since),
                trackRepository.count(),
                bookRepository.count(),
                playlistRepository.count(),
                playlistRepository.countByVisibility(PlaylistVisibility.PUBLIC),
                postRepository.count(),
                roomRepository.count(),
                listRepository.count(),
                playEventRepository.countByPlayedAtAfter(since),
                trackRepository.totalDurationSec(),
                bookRepository.totalPages());

        var storageInfo = new AdminOverviewResponse.Storage(
                musicBytes,
                booksBytes,
                musicBytes + booksBytes,
                storage.usableSpaceBytes());

        var newest = userRepository
                .findAll(PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(u -> AdminUserResponse.from(u, presenceService.isOnline(u.getId())))
                .toList();

        return new AdminOverviewResponse(
                totals,
                storageInfo,
                fillGaps(userRepository.createdAtSince(since), since),
                fillGaps(playEventRepository.playedAtSince(since), since),
                trackRepository.findTop12ByOrderByPlayCountDescIdDesc().stream().map(TrackResponse::from).toList(),
                trackRepository.findTop12ByOrderByCreatedAtDesc().stream().map(TrackResponse::from).toList(),
                bookRepository.findTop12ByOrderByReadCountDescIdDesc().stream().map(BookResponse::from).toList(),
                bookRepository.findTop12ByOrderByCreatedAtDesc().stream().map(BookResponse::from).toList(),
                nameCounts(trackRepository.genreSummaries(), 8),
                nameCounts(trackRepository.artistSummaries(), 8),
                newest);
    }

    // -------------------------------------------------------------- USUÁRIOS
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> users(String q, int page, int size) {
        String term = q == null ? "" : q.trim();
        var pageable = PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(userRepository.adminSearch(term, pageable),
                u -> AdminUserResponse.from(u, presenceService.isOnline(u.getId())));
    }

    /**
     * Troca o papel de alguém. O admin não pode rebaixar a si mesmo: seria o
     * jeito mais fácil de ficar sem nenhum admin no sistema por um clique errado.
     */
    @Transactional
    public AdminUserResponse changeRole(User actor, Long userId, String roleRaw) {
        User target = requireUser(userId);
        Role role = Role.from(roleRaw);
        if (!roleRaw.equalsIgnoreCase(role.name())) {
            throw ApiException.badRequest("Papel inválido (use \"user\" ou \"admin\")");
        }
        if (target.getId().equals(actor.getId()) && role != Role.ADMIN) {
            throw ApiException.badRequest("Você não pode tirar o seu próprio acesso de admin");
        }
        target.setRole(role);
        return AdminUserResponse.from(userRepository.save(target), presenceService.isOnline(target.getId()));
    }

    @Transactional
    public AdminUserResponse setSuspended(User actor, Long userId, boolean suspended) {
        User target = requireUser(userId);
        if (target.getId().equals(actor.getId())) {
            throw ApiException.badRequest("Você não pode suspender a si mesmo");
        }
        target.setSuspended(suspended);
        return AdminUserResponse.from(userRepository.save(target), presenceService.isOnline(target.getId()));
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));
    }

    // ---------------------------------------------------------------- HELPERS
    /**
     * Agrupa os instantes por dia (UTC) e completa com zero os dias sem nada.
     *
     * <p>O preenchimento importa: um gráfico alimentado só com os dias que
     * tiveram evento liga 12/08 em 03/09 como se fossem consecutivos e mente
     * sobre a tendência — a linha some justamente nos períodos parados.
     */
    private List<AdminOverviewResponse.DailyPoint> fillGaps(List<Instant> instants, Instant since) {
        Map<LocalDate, Long> byDate = new HashMap<>();
        for (Instant instant : instants) {
            if (instant == null) {
                continue;
            }
            LocalDate day = instant.atZone(ZoneOffset.UTC).toLocalDate();
            byDate.merge(day, 1L, Long::sum);
        }

        LocalDate start = since.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate today = Instant.now().atZone(ZoneOffset.UTC).toLocalDate();
        List<AdminOverviewResponse.DailyPoint> points = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(today); day = day.plusDays(1)) {
            points.add(new AdminOverviewResponse.DailyPoint(day.toString(), byDate.getOrDefault(day, 0L)));
        }
        return points;
    }

    private List<AdminOverviewResponse.NameCount> nameCounts(List<Object[]> rows, int limit) {
        List<AdminOverviewResponse.NameCount> out = new ArrayList<>();
        for (Object[] row : rows) {
            if (out.size() >= limit) {
                break;
            }
            // As duas consultas de agregação diferem: gênero devolve [nome, total]
            // e artista devolve [nome, capa, total]. O total é sempre o último.
            Object count = row[row.length - 1];
            out.add(new AdminOverviewResponse.NameCount((String) row[0], ((Number) count).longValue()));
        }
        return out;
    }
}

package com.movies.backend.media.local.service;

import com.movies.backend.media.gocine.dto.GocineTypes.CastMember;
import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.Episode;
import com.movies.backend.media.gocine.dto.GocineTypes.HomeFeed;
import com.movies.backend.media.gocine.dto.GocineTypes.HomeSection;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaItem;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaVideo;
import com.movies.backend.media.gocine.dto.GocineTypes.Season;
import com.movies.backend.media.local.entity.LocalEpisode;
import com.movies.backend.media.local.entity.LocalKind;
import com.movies.backend.media.local.entity.LocalTitle;
import com.movies.backend.media.local.entity.LocalTitleView;
import com.movies.backend.media.local.repository.LocalEpisodeRepository;
import com.movies.backend.media.local.repository.LocalImportItemRepository;
import com.movies.backend.media.local.repository.LocalTitleRepository;
import com.movies.backend.media.local.repository.LocalTitleViewRepository;
import com.movies.backend.post.repository.PostRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Catálogo servido 100% do banco local — não fala com o GoCine.
 *
 * Devolve os mesmos tipos do catálogo do GoCine (HomeFeed, MediaDetail...),
 * então o front troca de fonte só mudando o prefixo da URL.
 *
 * A NOTA é a do app: média das resenhas que o pessoal publicou. Título sem
 * resenha fica sem nota.
 */
@Service
public class LocalCatalogService {

    private static final int ROW = 20;
    private static final int TRENDING_DAYS = 7;

    private final LocalTitleRepository titles;
    private final LocalEpisodeRepository episodes;
    private final LocalImportItemRepository queue;
    private final LocalTitleViewRepository views;
    private final PostRepository posts;
    private final LocalImporter importer;
    private final ObjectMapper om;

    public LocalCatalogService(
            LocalTitleRepository titles,
            LocalEpisodeRepository episodes,
            LocalImportItemRepository queue,
            LocalTitleViewRepository views,
            PostRepository posts,
            LocalImporter importer,
            ObjectMapper om) {
        this.titles = titles;
        this.episodes = episodes;
        this.queue = queue;
        this.views = views;
        this.posts = posts;
        this.importer = importer;
        this.om = om;
    }

    /* ------------------------------------------------------------------ notas */

    /** Nota média do app por "movie:ID" / "tv:ID". */
    private record Rating(double avg, long count) {}

    private Map<String, Rating> ratings() {
        Map<String, Rating> out = new HashMap<>();
        for (Object[] r : posts.averageRatings()) {
            if (r[0] == null || r[1] == null || r[2] == null) continue;
            out.put(r[0] + ":" + r[1], new Rating(((Number) r[2]).doubleValue(), ((Number) r[3]).longValue()));
        }
        return out;
    }

    private static String ratingKey(LocalKind kind, long sourceId) {
        return kind.mediaType().json() + ":" + sourceId;
    }

    private static Double round(Rating r) {
        return r == null ? null : Math.round(r.avg() * 10) / 10d;
    }

    /* ------------------------------------------------------------------- home */

    public HomeFeed home() {
        Map<String, Rating> notes = ratings();
        List<HomeSection> sections = new ArrayList<>();

        List<MediaItem> trending = trending(notes);
        add(sections, "trending", "Bombando", trending);
        add(sections, "latest", "Lançados recentemente",
                items(titles.findByOrderByImportedAtDesc(PageRequest.of(0, ROW)), notes));
        add(sections, "top10movies", "Top 10 filmes", top10(LocalKind.MOVIE, notes));
        add(sections, "top10series", "Top 10 séries", top10(LocalKind.SERIES, notes));
        add(sections, "top10animes", "Top 10 animes", top10(LocalKind.ANIME, notes));
        add(sections, "movies", "Últimos filmes", latest(LocalKind.MOVIE, notes));
        add(sections, "series", "Últimas séries", latest(LocalKind.SERIES, notes));
        add(sections, "animes", "Últimos animes", latest(LocalKind.ANIME, notes));

        // destaque: o que está bombando; sem isso, os recém-chegados — só com imagem grande
        List<MediaItem> pool = new ArrayList<>(trending);
        if (!sections.isEmpty()) pool.addAll(sections.get(0).items());
        List<MediaItem> featured = pool.stream()
                .filter(i -> i.backdropUrl() != null && !i.backdropUrl().equals(i.posterUrl()))
                .distinct()
                .limit(5)
                .toList();
        return new HomeFeed(featured, sections);
    }

    private static void add(List<HomeSection> out, String key, String title, List<MediaItem> items) {
        if (!items.isEmpty()) out.add(new HomeSection(key, title, items));
    }

    private List<MediaItem> latest(LocalKind kind, Map<String, Rating> notes) {
        return items(titles.findByKindOrderByImportedAtDesc(kind, PageRequest.of(0, ROW)), notes);
    }

    /** Mais abertos por pessoas distintas nos últimos 7 dias. */
    private List<MediaItem> trending(Map<String, Rating> notes) {
        List<Object[]> rows = views.trendingSince(
                LocalDate.now().minusDays(TRENDING_DAYS), PageRequest.of(0, ROW));
        List<String> order = new ArrayList<>();
        Map<LocalKind, List<Long>> byKind = new HashMap<>();
        for (Object[] r : rows) {
            LocalKind k = (LocalKind) r[0];
            long id = ((Number) r[1]).longValue();
            order.add(k + ":" + id);
            byKind.computeIfAbsent(k, x -> new ArrayList<>()).add(id);
        }
        return inOrder(order, byKind, notes);
    }

    /** Os 10 de maior nota do app (desempate: mais resenhas). */
    private List<MediaItem> top10(LocalKind kind, Map<String, Rating> notes) {
        String prefix = kind.mediaType().json() + ":";
        List<Map.Entry<String, Rating>> ranked = notes.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .sorted(Comparator.comparingDouble((Map.Entry<String, Rating> e) -> e.getValue().avg())
                        .thenComparingLong(e -> e.getValue().count())
                        .reversed())
                .toList();
        List<Long> ids = ranked.stream()
                .map(e -> Long.parseLong(e.getKey().substring(prefix.length())))
                .toList();
        if (ids.isEmpty()) return List.of();
        // as resenhas não guardam se "tv" é série ou anime; o título local decide
        Map<Long, LocalTitle> found = new HashMap<>();
        for (LocalTitle t : titles.findByKindAndSourceIdIn(kind, ids)) found.put(t.getSourceId(), t);
        List<MediaItem> out = new ArrayList<>();
        for (Long id : ids) {
            LocalTitle t = found.get(id);
            if (t != null) out.add(item(t, notes));
            if (out.size() == 10) break;
        }
        return out;
    }

    private List<MediaItem> inOrder(
            List<String> order, Map<LocalKind, List<Long>> byKind, Map<String, Rating> notes) {
        Map<String, LocalTitle> found = new HashMap<>();
        byKind.forEach((k, ids) -> {
            for (LocalTitle t : titles.findByKindAndSourceIdIn(k, ids)) found.put(k + ":" + t.getSourceId(), t);
        });
        List<MediaItem> out = new ArrayList<>();
        for (String key : order) {
            LocalTitle t = found.get(key);
            if (t != null) out.add(item(t, notes));
        }
        return out;
    }

    private List<MediaItem> items(Collection<LocalTitle> ts, Map<String, Rating> notes) {
        return ts.stream().map(t -> item(t, notes)).toList();
    }

    private MediaItem item(LocalTitle t, Map<String, Rating> notes) {
        return new MediaItem(
                t.getSourceId(),
                t.getKind().mediaType(),
                t.getKind().contentKind(),
                t.getTitle(),
                t.getOverview() != null ? t.getOverview() : "",
                t.getPosterUrl(),
                t.getBackdropUrl(),
                round(notes.get(ratingKey(t.getKind(), t.getSourceId()))),
                t.getReleaseDate(),
                t.getGenre());
    }

    /* ------------------------------------------------------------------ busca */

    public List<MediaItem> search(String query, int limit) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) return List.of();
        Map<String, Rating> notes = ratings();
        return items(
                titles.findByTitleContainingIgnoreCaseOrderByImportedAtDesc(q, PageRequest.of(0, Math.min(limit, 60))),
                notes);
    }

    /* ---------------------------------------------------------------- detalhe */

    /** Título local; TV sem kind tenta série e depois anime. */
    private Optional<LocalTitle> find(MediaType type, ContentKind kind, long id) {
        Optional<LocalTitle> t = titles.findByKindAndSourceId(LocalKind.of(type, kind), id);
        if (t.isEmpty() && type == MediaType.TV && kind == null) {
            t = titles.findByKindAndSourceId(LocalKind.ANIME, id);
        }
        return t;
    }

    public MediaDetail detail(MediaType type, ContentKind kind, long id) {
        Optional<LocalTitle> found = find(type, kind, id);
        if (found.isEmpty()) return null;
        LocalTitle t = found.get();
        Map<String, Rating> notes = ratings();

        List<Season> seasons = new ArrayList<>();
        if (t.getKind() != LocalKind.MOVIE) {
            Map<Integer, List<LocalEpisode>> bySeason = new LinkedHashMap<>();
            for (LocalEpisode e : episodes.findByTitleIdOrderBySeasonNumberAscEpisodeNumberAsc(t.getId())) {
                bySeason.computeIfAbsent(e.getSeasonNumber(), x -> new ArrayList<>()).add(e);
            }
            bySeason.forEach((n, eps) -> {
                List<Episode> list = eps.stream()
                        .map(e -> new Episode(
                                e.getSourceEpisodeId() != null ? e.getSourceEpisodeId() : e.getId(),
                                e.getEpisodeNumber(),
                                e.getTitle() != null ? e.getTitle() : "Episódio " + e.getEpisodeNumber(),
                                e.getOverview() != null ? e.getOverview() : "",
                                e.getStillUrl(),
                                e.getRuntimeMinutes()))
                        .toList();
                String name = eps.get(0).getSeasonName();
                seasons.add(new Season(n, name != null ? name : "Temporada " + n, list.size(), list));
            });
        }

        return new MediaDetail(
                t.getSourceId(),
                t.getKind().mediaType(),
                t.getKind().contentKind(),
                t.getTitle(),
                t.getOverview() != null ? t.getOverview() : "",
                t.getPosterUrl(),
                t.getBackdropUrl(),
                round(notes.get(ratingKey(t.getKind(), t.getSourceId()))),
                t.getReleaseDate(),
                t.getGenre(),
                t.getRuntimeMinutes(),
                t.getTrailerUrl(),
                t.getGenres() == null || t.getGenres().isBlank()
                        ? List.of()
                        : Arrays.stream(t.getGenres().split(",")).map(String::trim).toList(),
                readList(t.getCastJson(), new TypeReference<List<CastMember>>() {}),
                List.of(),
                seasons);
    }

    /* ----------------------------------------------------------------- player */

    public List<MediaVideo> streams(MediaType type, ContentKind kind, long id, Integer season, Integer episode) {
        Optional<LocalTitle> found = find(type, kind, id);
        if (found.isEmpty()) return List.of();
        LocalTitle t = found.get();
        if (t.getKind() == LocalKind.MOVIE) {
            return readList(t.getStreamsJson(), new TypeReference<List<MediaVideo>>() {});
        }
        Optional<LocalEpisode> ep = season != null && episode != null
                ? episodes.findFirstByTitleIdAndSeasonNumberAndEpisodeNumber(t.getId(), season, episode)
                : episodes.findFirstByTitleIdOrderBySeasonNumberAscEpisodeNumberAsc(t.getId());
        return ep.map(e -> readList(e.getStreamsJson(), new TypeReference<List<MediaVideo>>() {}))
                .orElse(List.of());
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return om.readValue(json, type);
        } catch (Exception e) {
            return List.of();
        }
    }

    /* ------------------------------------------------------------- "bombando" */

    /** Marca que a pessoa abriu o título hoje (uma vez por dia conta). */
    public void recordView(MediaType type, ContentKind kind, long id, Long userId) {
        if (userId == null || type == null || id <= 0) return;
        LocalKind k = LocalKind.of(type, kind);
        LocalDate today = LocalDate.now();
        if (views.existsByKindAndSourceIdAndUserIdAndViewDay(k, id, userId, today)) return;
        try {
            views.save(new LocalTitleView(k, id, userId, today));
        } catch (DataIntegrityViolationException ignored) {
            // duas abas ao mesmo tempo: a outra já gravou
        }
    }

    /* ----------------------------------------------------------------- status */

    public record Status(
            long movies,
            long series,
            long animes,
            long episodes,
            long pending,
            long failed,
            String importing) {}

    public Status status() {
        return new Status(
                titles.countByKind(LocalKind.MOVIE),
                titles.countByKind(LocalKind.SERIES),
                titles.countByKind(LocalKind.ANIME),
                episodes.count(),
                queue.countByStatus(com.movies.backend.media.local.entity.LocalImportItem.Status.PENDING),
                queue.countByStatus(com.movies.backend.media.local.entity.LocalImportItem.Status.FAILED),
                importer.current());
    }
}

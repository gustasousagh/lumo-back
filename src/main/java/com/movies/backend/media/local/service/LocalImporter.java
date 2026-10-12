package com.movies.backend.media.local.service;

import com.movies.backend.media.gocine.dto.GocineTypes.Episode;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaVideo;
import com.movies.backend.media.gocine.dto.GocineTypes.Season;
import com.movies.backend.media.gocine.service.GocineService.CatalogEntry;
import com.movies.backend.media.gocine.service.GocineService;
import com.movies.backend.media.local.config.LocalImportProperties;
import com.movies.backend.media.local.entity.LocalEpisode;
import com.movies.backend.media.local.entity.LocalImportCursor;
import com.movies.backend.media.local.entity.LocalImportItem.Status;
import com.movies.backend.media.local.entity.LocalImportItem;
import com.movies.backend.media.local.entity.LocalImportState;
import com.movies.backend.media.local.entity.LocalKind;
import com.movies.backend.media.local.repository.LocalImportCursorRepository;
import com.movies.backend.media.local.repository.LocalImportItemRepository;
import com.movies.backend.media.local.repository.LocalImportStateRepository;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Rotina que copia o catálogo do GoCine para o banco local, devagar.
 *
 * A cada tick (1 min por padrão) faz UMA coisa:
 *
 *   - se a fila está curta, lê a próxima página (12 títulos) da listagem de um
 *     dos catálogos — filmes, séries, animes, em rodízio — e enfileira os novos;
 *   - senão, importa o próximo título da fila: detalhe + player. Série e anime
 *     vêm completos, todas as temporadas, com o player de cada episódio — um
 *     episódio a cada `episodeDelayMs`, para não martelar o site.
 *
 * Quando a listagem de um catálogo acaba, ele dorme `refreshHours` e depois
 * relê do começo só até a primeira página sem nada novo.
 *
 * Tudo que importa (cursores, fila, pausa) fica no banco: reiniciar o Pi
 * retoma de onde parou.
 *
 * SEM REPETIÇÃO: a fila e os títulos têm chave única (kind, id do GoCine); a
 * descoberta pula o que já está na fila, e título DONE nunca volta para ela.
 *
 * Roda numa thread só sua. O agendador padrão do Spring é o mesmo do broker do
 * WebSocket, e as pausas entre episódios travariam o tempo real do app.
 */
@Component
public class LocalImporter {

    private static final Logger log = LoggerFactory.getLogger(LocalImporter.class);

    /** Nome da listagem completa de cada catálogo no BFF do GoCine. */
    private static final Map<LocalKind, String> LISTS =
            Map.of(
                    LocalKind.MOVIE, "latestMovies",
                    LocalKind.SERIES, "latestseries",
                    LocalKind.ANIME, "latestanimes");

    /** Abaixo disso a próxima rodada descobre títulos em vez de importar. */
    private static final int QUEUE_LOW_WATER = 24;

    private final GocineService gocine;
    private final LocalCatalogWriter writer;
    private final LocalImportItemRepository queue;
    private final LocalImportCursorRepository cursors;
    private final LocalImportProperties props;
    private final ObjectMapper om;

    /** O que está sendo importado agora, para o status no header. */
    private volatile String current;

    private final LocalImportStateRepository state;
    private volatile boolean paused;
    private ScheduledExecutorService worker;

    public LocalImporter(
            GocineService gocine,
            LocalCatalogWriter writer,
            LocalImportItemRepository queue,
            LocalImportCursorRepository cursors,
            LocalImportProperties props,
            LocalImportStateRepository state,
            ObjectMapper om) {
        this.gocine = gocine;
        this.writer = writer;
        this.queue = queue;
        this.cursors = cursors;
        this.props = props;
        this.state = state;
        this.om = om;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!props.isEnabled()) return;
        paused = state.findById(LocalImportState.SINGLETON).map(LocalImportState::isPaused).orElse(false);
        worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "local-import");
            t.setDaemon(true);
            return t;
        });
        worker.scheduleWithFixedDelay(this::tick, props.getInitialDelayMs(), props.getTickMs(), TimeUnit.MILLISECONDS);
        log.info("Importação local ligada: a cada {}s{}", props.getTickMs() / 1000, paused ? " (pausada)" : "");
    }

    @PreDestroy
    public void stop() {
        if (worker != null) worker.shutdownNow();
    }

    public boolean isPaused() {
        return paused;
    }

    /** Pausa ou retoma. Uma série no meio para no próximo episódio e recomeça depois. */
    public void setPaused(boolean value) {
        LocalImportState s = state.findById(LocalImportState.SINGLETON).orElseGet(LocalImportState::new);
        s.setPaused(value);
        state.save(s);
        paused = value;
        log.info("Importação local {}", value ? "pausada" : "retomada");
    }

    public String current() {
        return current;
    }

    void tick() {
        if (paused) return;
        try {
            if (queue.countByStatus(Status.PENDING) < QUEUE_LOW_WATER && discoverOnce()) return;
            importNext();
        } catch (Throwable e) {
            // Throwable de propósito: no scheduleWithFixedDelay, qualquer coisa que
            // escape cancela a rotina para sempre, sem aviso
            log.warn("Importação local: {}", e.toString());
        }
    }

    /* ------------------------------------------------------------ descoberta */

    /** Lê uma página da listagem. false se todos os catálogos estão em dia. */
    boolean discoverOnce() {
        Instant now = Instant.now();
        Optional<LocalImportCursor> next =
                LISTS.keySet().stream()
                        .map(k -> cursors.findById(k).orElseGet(() -> new LocalImportCursor(k)))
                        .filter(c -> !c.isExhausted()
                                || (c.getRefreshAt() != null && !now.isBefore(c.getRefreshAt())))
                        .min(Comparator.comparing(LocalImportCursor::getLastRunAt));
        if (next.isEmpty()) return false;

        LocalImportCursor c = next.get();
        if (c.isExhausted()) {
            // hora de procurar títulos novos: recomeça do topo
            c.setExhausted(false);
            c.setRefreshing(true);
            c.setNextPage(1);
        }
        c.setLastRunAt(now);

        List<CatalogEntry> page = gocine.listCatalog(LISTS.get(c.getKind()), c.getNextPage());
        int fresh = 0;
        for (CatalogEntry e : page) {
            if (queue.existsByKindAndSourceId(c.getKind(), e.id())) continue;
            LocalImportItem item = new LocalImportItem();
            item.setKind(c.getKind());
            item.setSourceId(e.id());
            item.setTitle(LocalCatalogWriter.cut(e.title(), 255));
            queue.save(item);
            fresh++;
        }

        if (page.isEmpty() || (c.isRefreshing() && fresh == 0)) {
            c.setExhausted(true);
            c.setRefreshing(false);
            c.setRefreshAt(now.plus(Duration.ofHours(props.getRefreshHours())));
            log.info("Listagem de {} em dia (parou na página {}).", c.getKind(), c.getNextPage());
        } else {
            c.setNextPage(c.getNextPage() + 1);
        }
        cursors.save(c);
        return true;
    }

    /* ------------------------------------------------------------ importação */

    void importNext() {
        Optional<LocalImportItem> next = queue.findFirstByStatusOrderByAttemptsAscIdAsc(Status.PENDING);
        if (next.isEmpty()) return;
        LocalImportItem item = next.get();
        current = item.getKind() + " " + (item.getTitle() != null ? item.getTitle() : item.getSourceId());
        long started = System.currentTimeMillis();
        try {
            boolean found = item.getKind() == LocalKind.MOVIE
                    ? importMovie(item.getSourceId())
                    : importTv(item.getKind(), item.getSourceId());
            item.setStatus(found ? Status.DONE : Status.GONE);
            item.setLastError(null);
            log.info("Importado {} em {}s", current, (System.currentTimeMillis() - started) / 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return; // desligando: fica PENDING e retoma no próximo arranque
        } catch (PausedException e) {
            log.info("Pausado no meio de {}; recomeça quando retomar", current);
            return; // fica PENDING, sem gastar tentativa
        } catch (Exception e) {
            item.setAttempts(item.getAttempts() + 1);
            item.setLastError(LocalCatalogWriter.cut(e.toString(), 500));
            if (item.getAttempts() >= props.getMaxAttempts()) item.setStatus(Status.FAILED);
            log.warn("Falhou importar {} (tentativa {}): {}", current, item.getAttempts(), e.toString());
        } finally {
            current = null;
        }
        item.setUpdatedAt(Instant.now());
        queue.save(item);
    }

    private boolean importMovie(long id) {
        MediaDetail d = gocine.getMovieDetail(id);
        if (d == null) return false;
        List<MediaVideo> streams = gocine.resolveStreams(id, MediaType.MOVIE, null, null, null);
        if (streams.isEmpty()) throw new IllegalStateException("filme sem player");
        writer.save(LocalKind.MOVIE, d, om.writeValueAsString(streams), null);
        return true;
    }

    private boolean importTv(LocalKind kind, long id) throws InterruptedException {
        MediaDetail d = gocine.getSeriesDetail(id, kind.contentKind());
        if (d == null) return false;

        List<LocalEpisode> eps = new ArrayList<>();
        int failed = 0;
        for (Season s : d.seasons()) {
            for (Episode ep : s.episodes()) {
                if (paused) throw new PausedException();
                Thread.sleep(props.getEpisodeDelayMs());
                List<MediaVideo> streams = episodeStreams(kind, id, s.seasonNumber(), ep.episodeNumber());
                if (streams.isEmpty()) failed++;
                LocalEpisode le = new LocalEpisode();
                le.setSeasonNumber(s.seasonNumber());
                le.setSeasonName(LocalCatalogWriter.cut(s.name(), 255));
                le.setEpisodeNumber(ep.episodeNumber());
                le.setSourceEpisodeId(ep.id());
                le.setTitle(LocalCatalogWriter.cut(ep.title(), 255));
                le.setOverview(ep.overview());
                le.setStillUrl(LocalCatalogWriter.cut(ep.stillUrl(), 500));
                le.setRuntimeMinutes(ep.runtimeMinutes());
                le.setStreamsJson(om.writeValueAsString(streams));
                eps.add(le);
            }
        }
        if (!eps.isEmpty() && failed == eps.size()) {
            throw new IllegalStateException("nenhum episódio com player (" + failed + ")");
        }
        writer.save(kind, d, null, eps);
        return true;
    }

    /** Pausaram no meio de uma série. */
    private static final class PausedException extends RuntimeException {}

    /** Player de um episódio, com uma segunda tentativa; vazio se não houver. */
    private List<MediaVideo> episodeStreams(LocalKind kind, long id, int season, int episode)
            throws InterruptedException {
        for (int attempt = 1; ; attempt++) {
            try {
                return gocine.resolveStreams(id, MediaType.TV, kind.contentKind(), season, episode);
            } catch (Exception e) {
                if (attempt >= 2) return List.of();
                Thread.sleep(props.getEpisodeDelayMs() * 2);
            }
        }
    }
}

package com.movies.backend.media.local.service;

import com.movies.backend.media.gocine.dto.GocineTypes.Episode;
import com.movies.backend.media.gocine.exception.GocineUnavailableException;
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
 * A cada tick faz UMA coisa (5 s entre uma e outra enquanto houver trabalho;
 * com tudo em dia, só olha de novo a cada `idleMs`, 5 min por padrão):
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
 * VARREDURA POR ID: a listagem do GoCine para em ~400 páginas (os ~4.800 mais
 * recentes), e os títulos antigos nunca aparecem nela. Então, terminada a
 * listagem, o catálogo passa a enfileirar id por id, do maior já visto até 1,
 * pulando o que já está na fila. Id que não existe vira GONE com uma consulta.
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

    /** Ids enfileirados por rodada de varredura. */
    private static final int SWEEP_BATCH = 24;

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

    /** Sem sessão no GoCine: espera até aqui antes de tentar de novo. */
    private static final Duration AUTH_BACKOFF = Duration.ofMinutes(5);
    private volatile Instant backoffUntil = Instant.EPOCH;
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
        worker.schedule(this::loop, props.getInitialDelayMs(), TimeUnit.MILLISECONDS);
        log.info("Importação local ligada: a cada {}s com trabalho, {}s parada{}",
                props.getTickMs() / 1000, props.getIdleMs() / 1000, paused ? " (pausada)" : "");
    }

    /** Uma rodada e agenda a próxima: rápido se fez algo, devagar se não havia nada. */
    private void loop() {
        boolean worked = false;
        try {
            worked = tick();
        } finally {
            if (!worker.isShutdown()) {
                worker.schedule(this::loop, worked ? props.getTickMs() : props.getIdleMs(), TimeUnit.MILLISECONDS);
            }
        }
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

    /** Uma rodada. true quando havia trabalho (descobriu uma página ou importou um título). */
    boolean tick() {
        if (paused || Instant.now().isBefore(backoffUntil)) return false;
        try {
            if (queue.countByStatus(Status.PENDING) < QUEUE_LOW_WATER && discoverOnce()) return true;
            return importNext();
        } catch (GocineUnavailableException e) {
            backOff(e);
            return false;
        } catch (Throwable e) {
            // Throwable de propósito: o que escapasse daqui mataria a thread da rotina
            log.warn("Importação local: {}", e.toString());
            return false;
        }
    }

    /** O GoCine está fora ou limitando: espera, sem culpar o título. */
    private void backOff(GocineUnavailableException e) {
        backoffUntil = Instant.now().plus(AUTH_BACKOFF);
        log.warn("GoCine indisponível ({}); importação espera {} min", e.getMessage(), AUTH_BACKOFF.toMinutes());
    }

    /* ------------------------------------------------------------ descoberta */

    /** Lê uma página da listagem. false se todos os catálogos estão em dia. */
    boolean discoverOnce() {
        Instant now = Instant.now();
        Optional<LocalImportCursor> next =
                LISTS.keySet().stream()
                        .map(k -> cursors.findById(k).orElseGet(() -> new LocalImportCursor(k)))
                        .filter(c -> !c.isExhausted() || refreshDue(c, now) || sweepPending(c))
                        .min(Comparator.comparing(LocalImportCursor::getLastRunAt));
        if (next.isEmpty()) return false;

        LocalImportCursor c = next.get();
        if (c.isExhausted() && !refreshDue(c, now)) {
            c.setLastRunAt(now);
            sweep(c);
            cursors.save(c);
            return true;
        }
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

    private static boolean refreshDue(LocalImportCursor c, Instant now) {
        return c.isExhausted() && c.getRefreshAt() != null && !now.isBefore(c.getRefreshAt());
    }

    /** Listagem já percorrida inteira uma vez e a varredura por id ainda não acabou. */
    private boolean sweepPending(LocalImportCursor c) {
        return props.isSweepEnabled() && c.isExhausted() && !c.isRefreshing()
                && (c.getSweepNextId() == null || c.getSweepNextId() > 0);
    }

    /** Enfileira o próximo lote de ids que a listagem não mostrou. */
    private void sweep(LocalImportCursor c) {
        Long from = c.getSweepNextId();
        if (from == null) {
            Long max = queue.maxSourceId(c.getKind());
            from = max == null ? 0 : max;
            log.info("Listagem de {} acabou; varrendo ids de {} até 1", c.getKind(), from);
        }
        long id = from;
        int added = 0;
        for (; id > 0 && added < SWEEP_BATCH; id--) {
            if (queue.existsByKindAndSourceId(c.getKind(), id)) continue;
            LocalImportItem item = new LocalImportItem();
            item.setKind(c.getKind());
            item.setSourceId(id);
            queue.save(item);
            added++;
        }
        c.setSweepNextId(id);
        if (id == 0) log.info("Varredura de {} terminou", c.getKind());
    }

    /* ------------------------------------------------------------ importação */

    /** Importa o próximo da fila. false se a fila estava vazia. */
    boolean importNext() {
        Optional<LocalImportItem> next = queue.findFirstByStatusOrderByAttemptsAscIdAsc(Status.PENDING);
        if (next.isEmpty()) return false;
        LocalImportItem item = next.get();
        current = item.getKind() + " " + (item.getTitle() != null ? item.getTitle() : item.getSourceId());
        long started = System.currentTimeMillis();
        try {
            boolean found = item.getKind() == LocalKind.MOVIE
                    ? importMovie(item.getSourceId())
                    : importTv(item.getKind(), item.getSourceId(),
                            item.getAttempts() + 1 >= props.getMaxAttempts());
            item.setStatus(found ? Status.DONE : Status.GONE);
            item.setLastError(null);
            log.info("Importado {} em {}s", current, (System.currentTimeMillis() - started) / 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false; // desligando: fica PENDING e retoma no próximo arranque
        } catch (GocineUnavailableException e) {
            // GoCine fora, sem sessão ou limitando: não é culpa do título — não
            // gasta tentativa, e a série volta inteira depois (nada pela metade)
            backOff(e);
            return false;
        } catch (PausedException e) {
            log.info("Pausado no meio de {}; recomeça quando retomar", current);
            return false; // fica PENDING, sem gastar tentativa
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
        return true;
    }

    private boolean importMovie(long id) {
        MediaDetail d = gocine.getMovieDetail(id);
        if (d == null) return false;
        requireContent(d);
        List<MediaVideo> streams = gocine.resolveStreams(id, MediaType.MOVIE, null, null, null);
        if (streams.isEmpty()) throw new IllegalStateException("filme sem player");
        writer.save(LocalKind.MOVIE, d, om.writeValueAsString(streams), null);
        return true;
    }

    /**
     * @param lastAttempt na última tentativa, salva mesmo com episódios sem
     *     player — melhor a série com um buraco do que sem a série
     */
    private boolean importTv(LocalKind kind, long id, boolean lastAttempt) throws InterruptedException {
        MediaDetail d = gocine.getSeriesDetail(id, kind.contentKind());
        if (d == null) return false;
        requireContent(d);
        if (d.seasons().isEmpty() || d.seasons().stream().allMatch(s -> s.episodes().isEmpty())) {
            throw new IllegalStateException("série veio sem episódios");
        }

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
        if (failed == eps.size()) {
            throw new IllegalStateException("nenhum episódio com player (" + failed + ")");
        }
        if (failed > 0 && !lastAttempt) {
            throw new IllegalStateException(failed + " de " + eps.size() + " episódios sem player");
        }
        if (failed > 0) log.warn("{}: salvando com {} episódio(s) sem player", current, failed);
        writer.save(kind, d, null, eps);
        return true;
    }

    /**
     * Página que veio sem os dados (erro momentâneo do GoCine) não pode ser
     * salva como se fosse o título: vira erro e é tentada de novo.
     */
    private static void requireContent(MediaDetail d) {
        if (d.title() == null || d.title().isBlank() || "Sem título".equals(d.title())) {
            throw new IllegalStateException("página do título veio sem dados");
        }
    }

    /** Pausaram no meio de uma série. */
    private static final class PausedException extends RuntimeException {}

    /** Player de um episódio, com uma segunda tentativa; vazio se não houver. */
    private List<MediaVideo> episodeStreams(LocalKind kind, long id, int season, int episode)
            throws InterruptedException {
        for (int attempt = 1; ; attempt++) {
            try {
                return gocine.resolveStreams(id, MediaType.TV, kind.contentKind(), season, episode);
            } catch (GocineUnavailableException e) {
                throw e; // GoCine fora/limitando: não grava episódio sem player
            } catch (Exception e) {
                if (attempt >= 2) return List.of();
                Thread.sleep(props.getEpisodeDelayMs() * 2);
            }
        }
    }
}

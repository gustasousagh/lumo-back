package com.movies.backend.media.gocine.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.movies.backend.media.gocine.client.GocineWebClient;
import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.Episode;
import com.movies.backend.media.gocine.dto.GocineTypes.HomeFeed;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaItem;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaVideo;
import com.movies.backend.media.gocine.exception.GocineNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

/**
 * Catálogo do GoCine: home, busca, detalhe e resolução de stream.
 *
 * O GoCine migrou de um `siteapi` com token fixo (que saiu do ar) para um site
 * autenticado (web.gocine.app). Agora os dados vêm de duas formas:
 *
 *   - BFF deles (/api/bff/...), JSON limpo nos nomes de campo antigos: busca e
 *     a lista de episódios de cada temporada.
 *   - Páginas Next renderizadas pelo servidor (/, /movie/ID, /series/ID,
 *     /anime/ID, /watch/...). O {@link GocineWebClient} faz o login e extrai o
 *     "flight" RSC; o mapper recorta o que interessa.
 *
 * ANIME NÃO É SÉRIE: no GoCine os animes vivem numa tabela própria, com ids que
 * colidem com os de série (683 é "Naruto" em /anime e outra coisa em /series).
 * Por isso toda URL de série/anime sai de {@link #tvSegment} pelo ContentKind.
 */
@Service
public class GocineService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GocineService.class);

    /** Imagem grande do topo da página de detalhe (o og:image e o hero). */
    private static final Pattern BACKDROP =
            Pattern.compile("\"(https://image\\.tmdb\\.org/t/p/(?:w1280|original)/[^\"]+)\"");

    private final GocineWebClient client;
    private final GocineMapper mapper;
    private final ObjectMapper om;

    /** Busca das temporadas em paralelo: um anime longo tem dezenas delas. */
    private final ExecutorService seasonPool = Executors.newFixedThreadPool(6);

    public GocineService(GocineWebClient client, GocineMapper mapper, ObjectMapper om) {
        this.client = client;
        this.mapper = mapper;
        this.om = om;
    }

    /** "anime" ou "series" — o segmento de URL de um título de TV. */
    static String tvSegment(ContentKind kind) {
        return kind == ContentKind.ANIME ? "anime" : "series";
    }

    /**
     * Home ao vivo quando dá, curada quando não dá.
     *
     * As linhas vêm na prop `rows` do componente da home; os destaques do topo
     * são o array `items` que fica antes do primeiro `customLink` (só o
     * carrossel de destaque tem esse campo). Se a página mudar e nada vier, cai
     * na home curada empacotada, para a tela nunca ficar vazia.
     */
    public HomeFeed getHome() {
        try {
            String flight = client.page("/", true);
            JsonNode rows = client.pageArray(flight, "rows");
            JsonNode featured = client.pageArrayBefore(flight, "\"customLink\"", "items");
            HomeFeed feed = mapper.homeFromRows(rows, featured);
            if (!feed.sections().isEmpty()) return feed;
            log.warn("Home ao vivo veio sem linhas (rows={}), usando a curada",
                    rows == null ? "ausente" : rows.size());
        } catch (Exception e) {
            log.warn("Home ao vivo falhou, usando a curada: {}", e.toString());
        }
        return curatedHome();
    }

    /** Home curada empacotada em resources/gocine/home-fallback.json. */
    private HomeFeed curatedHome() {
        try (java.io.InputStream in = getClass().getResourceAsStream("/gocine/home-fallback.json")) {
            if (in == null) return new HomeFeed(List.of(), List.of());
            return mapper.homeFromRows(om.readTree(in).get("rows"), null);
        } catch (Exception e) {
            log.warn("Home curada indisponível: {}", e.toString());
            return new HomeFeed(List.of(), List.of());
        }
    }

    public List<MediaItem> search(String query, int limit) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) return List.of();
        String path = "search/" + UriUtils.encodePathSegment(q, StandardCharsets.UTF_8);
        try {
            return mapper.toSearchResults(client.bff(path), limit);
        } catch (GocineNotFoundException e) {
            return List.of();
        }
    }

    /** Um título da listagem do catálogo. */
    public record CatalogEntry(long id, String title) {}

    /**
     * Uma página (12 itens, mais novos primeiro) da listagem completa de um
     * catálogo: "latestMovies", "latestseries" ou "latestanimes". Lista vazia
     * quando a página passou do fim.
     */
    public List<CatalogEntry> listCatalog(String list, int page) {
        JsonNode res = client.bff("genres/" + list + "/all?page=" + page);
        List<CatalogEntry> out = new ArrayList<>();
        for (JsonNode n : res.path("data")) {
            long id = n.path("id").asLong(0);
            if (id <= 0) continue;
            String title = n.path("title").asString(null);
            if (title == null) title = n.path("name").asString(null);
            out.add(new CatalogEntry(id, title));
        }
        return out;
    }

    /** Detalhe de filme. Devolve null quando o GoCine não conhece o id. */
    public MediaDetail getMovieDetail(long id) {
        String flight = client.page("/movie/" + id, true);
        if (notFound(flight)) return null;
        return mapper.movieDetailFromWeb(
                client.pageObjectContaining(flight, "\"meta\":\""),
                client.pageObjectContaining(flight, "\"providersUrl\""),
                client.pageObjectContaining(flight, "\"@context\":\"https://schema.org\""),
                client.pageMatch(flight, BACKDROP),
                id);
    }

    /**
     * Detalhe de série/anime, já com os episódios de TODAS as temporadas — a
     * tela do front lista e toca a partir daqui, sem pedir mais nada.
     *
     * A página só traz a lista de temporadas (e os episódios da primeira); o
     * resto vem do BFF, uma chamada por temporada.
     */
    public MediaDetail getSeriesDetail(long id, ContentKind preferredKind) {
        ContentKind kind = preferredKind != null ? preferredKind : ContentKind.SERIES;
        String flight = client.page("/" + tvSegment(kind) + "/" + id, true);
        if (notFound(flight)) return null;

        JsonNode seasonsBox = client.pageObjectContaining(flight, "\"titleId\":");
        JsonNode seasons = seasonsBox == null ? null : seasonsBox.get("seasons");
        List<GocineMapper.SeasonRef> refs = mapper.seasonRefs(seasons);

        String bffPrefix = kind == ContentKind.ANIME ? "animes" : "series";
        List<CompletableFuture<List<Episode>>> pending = new ArrayList<>();
        for (GocineMapper.SeasonRef ref : refs) {
            pending.add(
                    CompletableFuture.supplyAsync(
                            () -> loadSeason(bffPrefix, ref),
                            seasonPool));
        }
        List<List<Episode>> episodes = pending.stream().map(CompletableFuture::join).toList();

        return mapper.seriesDetailFromWeb(
                client.pageObjectContaining(flight, "\"meta\":\""),
                client.pageObjectContaining(flight, "\"providersUrl\""),
                client.pageObjectContaining(flight, "\"@context\":\"https://schema.org\""),
                client.pageMatch(flight, BACKDROP),
                refs,
                episodes,
                id,
                kind);
    }

    /** Episódios de uma temporada; vazio se o BFF falhar, para não derrubar o detalhe. */
    private List<Episode> loadSeason(String bffPrefix, GocineMapper.SeasonRef ref) {
        try {
            return mapper.episodesFromBff(client.bff(bffPrefix + "/season/" + ref.id()));
        } catch (Exception e) {
            log.warn("Temporada {} ({}) não carregou: {}", ref.number(), ref.id(), e.toString());
            return List.of();
        }
    }

    /**
     * Fontes de vídeo. A página /watch resolve o link já com token novo; por
     * isso NÃO é cacheada — o token tem vida curta.
     *
     * `episode` é o número do episódio como o GoCine numera — em anime é a
     * numeração absoluta (a temporada 2 de Naruto começa no 53), que é o mesmo
     * número que o detalhe devolve.
     */
    public List<MediaVideo> resolveStreams(
            long id,
            MediaType mediaType,
            ContentKind contentKind,
            Integer seasonNumber,
            Integer episodeNumber) {

        String path;
        if (mediaType == MediaType.MOVIE) {
            path = "/watch/movie/" + id;
        } else {
            int s = seasonNumber != null ? seasonNumber : 1;
            int e = episodeNumber != null ? episodeNumber : 1;
            path = "/watch/" + tvSegment(contentKind) + "/" + id + "?season=" + s + "&episode=" + e;
        }
        String flight = client.page(path, false);
        JsonNode sources = client.pageArray(flight, "sources");
        return mapper.streamsFromSources(sources);
    }

    private static boolean notFound(String flight) {
        return flight != null
                && (flight.contains("Não encontramos") || flight.contains("não existe mais"));
    }
}

package com.movies.backend.media.gocine;

import static org.assertj.core.api.Assertions.assertThat;

import com.movies.backend.media.gocine.client.GocineWebClient;
import com.movies.backend.media.gocine.config.GocineProperties;
import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.HomeFeed;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaVideo;
import com.movies.backend.media.gocine.dto.GocineTypes.Season;
import com.movies.backend.media.gocine.service.GocineMapper;
import com.movies.backend.media.gocine.service.GocineService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Caminho completo — login, HTTP, recorte do RSC e orquestração — contra um
 * stub do web.gocine.app servido pelo próprio teste, com páginas reais
 * gravadas em src/test/resources/gocine/web. Sem rede.
 */
class GocineServiceTest {

    /** Rota (path + query) -> fixture. */
    private static final Map<String, String> ROUTES =
            Map.ofEntries(
                    Map.entry("/", "home.html"),
                    Map.entry("/movie/33697", "movie-33697.html"),
                    Map.entry("/series/14173", "series-14173.html"),
                    Map.entry("/anime/683", "anime-683.html"),
                    Map.entry("/watch/anime/683?season=2&episode=53", "watch-anime-683-s2e53.html"),
                    Map.entry("/watch/series/14173?season=1&episode=1", "watch-series-14173-s1e1.html"),
                    Map.entry("/api/bff/series/season/33138", "bff-series-season.json"),
                    Map.entry("/api/bff/animes/season/2017", "bff-animes-season-2017.json"),
                    Map.entry("/api/bff/animes/season/2018", "bff-animes-season-2018.json"),
                    Map.entry("/api/bff/animes/season/2019", "bff-animes-season-2019.json"),
                    Map.entry("/api/bff/animes/season/2020", "bff-animes-season-2020.json"));

    private HttpServer server;
    private GocineService service;
    private final ObjectMapper om = JsonMapper.builder().build();
    private final List<String> requests = new CopyOnWriteArrayList<>();

    private byte[] fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/gocine/web/" + name)) {
            return in.readAllBytes();
        }
    }

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        server.start();

        GocineProperties props = new GocineProperties();
        props.setWebUrl("http://127.0.0.1:" + server.getAddress().getPort());
        props.setEmail("teste@example.com");
        props.setPassword("senha");
        props.setProfileId(1);
        service = new GocineService(new GocineWebClient(props, om), new GocineMapper(), om);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String uri = exchange.getRequestURI().toString();
        requests.add(exchange.getRequestMethod() + " " + uri);
        byte[] body;
        int status = 200;
        if (uri.equals("/api/auth/login") || uri.equals("/api/profile/select")) {
            exchange.getResponseHeaders().add("Set-Cookie", "gc_sess=abc; Path=/");
            body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
        } else if (ROUTES.containsKey(uri)) {
            body = fixture(ROUTES.get(uri));
        } else {
            status = 404;
            body = "{\"message\":\"not found\"}".getBytes(StandardCharsets.UTF_8);
        }
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    @AfterEach
    void stopStub() {
        if (server != null) server.stop(0);
    }

    @Test
    @DisplayName("home ao vivo: linhas de filme, série e anime + destaques, e serializa")
    void home() {
        HomeFeed feed = service.getHome();
        assertThat(feed.sections()).hasSizeGreaterThanOrEqualTo(10);
        assertThat(feed.sections()).anySatisfy(s -> assertThat(s.title()).isEqualTo("Top 10 animes"));
        assertThat(feed.sections().stream().flatMap(s -> s.items().stream()))
                .anySatisfy(i -> assertThat(i.contentKind()).isEqualTo(ContentKind.ANIME))
                .anySatisfy(i -> assertThat(i.contentKind()).isEqualTo(ContentKind.SERIES))
                .anySatisfy(i -> assertThat(i.mediaType()).isEqualTo(MediaType.MOVIE));
        // os destaques são o carrossel do topo, não a primeira linha
        assertThat(feed.featured()).hasSize(5);
        assertThat(feed.featured().get(0).backdropUrl()).contains("w1280");
        assertThat(om.writeValueAsString(feed)).contains("\"contentKind\":\"anime\"");
    }

    @Test
    @DisplayName("anime abre em /anime, com todas as temporadas e numeração absoluta")
    void animeDetail() {
        MediaDetail d = service.getSeriesDetail(683, ContentKind.ANIME);
        assertThat(d.title()).isEqualTo("Naruto");
        assertThat(d.contentKind()).isEqualTo(ContentKind.ANIME);
        assertThat(d.overview()).contains("Kyuubi");
        assertThat(d.voteAverage()).isEqualTo(8.4);
        assertThat(d.genres()).contains("Animação");
        assertThat(d.seasons()).extracting(Season::seasonNumber).containsExactly(1, 2, 3, 4);
        assertThat(d.seasons()).allSatisfy(s -> assertThat(s.episodes()).isNotEmpty());
        assertThat(d.seasons().get(1).episodes().get(0).episodeNumber()).isEqualTo(53);
        assertThat(requests).noneMatch(r -> r.contains("/series/683"));
    }

    @Test
    @DisplayName("série traz os episódios da temporada")
    void seriesDetail() {
        MediaDetail d = service.getSeriesDetail(14173, ContentKind.SERIES);
        assertThat(d.title()).isEqualTo("A Ilha do Tesouro");
        assertThat(d.overview()).contains("Jim Hawkins");
        assertThat(d.seasons()).hasSize(1);
        assertThat(d.seasons().get(0).episodes()).hasSize(1);
        assertThat(d.seasons().get(0).episodeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("filme: sinopse, gêneros, duração e backdrop")
    void movieDetail() {
        MediaDetail d = service.getMovieDetail(33697);
        assertThat(d.title()).isEqualTo("Sobrevivência");
        assertThat(d.overview()).startsWith("Quando o filho");
        assertThat(d.genres()).containsExactly("Drama", "Thriller");
        assertThat(d.runtimeMinutes()).isEqualTo(106);
        assertThat(d.backdropUrl()).contains("w1280");
        assertThat(d.cast()).isNotEmpty();
    }

    @Test
    @DisplayName("stream de anime vai para /watch/anime com o número do episódio do GoCine")
    void animeStream() {
        List<MediaVideo> v = service.resolveStreams(683, MediaType.TV, ContentKind.ANIME, 2, 53);
        assertThat(v).hasSize(1);
        assertThat(v.get(0).link()).contains(".mp4?token=");
        assertThat(v.get(0).server()).isEqualTo("Dublado");
    }

    @Test
    @DisplayName("stream de série vai para /watch/series")
    void seriesStream() {
        List<MediaVideo> v = service.resolveStreams(14173, MediaType.TV, ContentKind.SERIES, 1, 1);
        assertThat(v).isNotEmpty();
    }

    @Test
    @DisplayName("loga uma vez só e reaproveita a sessão")
    void logsInOnce() {
        service.getHome();
        service.getMovieDetail(33697);
        assertThat(requests).filteredOn(r -> r.endsWith("/api/auth/login")).hasSize(1);
    }

    @Test
    @DisplayName("busca com menos de 2 caracteres não chama a origem")
    void shortQueryShortCircuits() {
        assertThat(service.search("a", 30)).isEmpty();
        assertThat(service.search("  ", 30)).isEmpty();
        assertThat(requests).isEmpty();
    }
}

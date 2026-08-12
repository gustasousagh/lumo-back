package com.movies.backend.media.gocine;

import static org.assertj.core.api.Assertions.assertThat;

import com.movies.backend.media.gocine.client.GocineClient;
import com.movies.backend.media.gocine.config.GocineProperties;
import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaVideo;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cobre o caminho completo — HTTP, parsing e orquestração — contra um stub do
 * GoCine servido pelo próprio teste. Sem rede: roda em qualquer CI.
 */
class GocineServiceTest {

    private static final Map<String, String> ROUTES =
            Map.of(
                    "/media/mobile/default", "raw-home.json",
                    "/search/matrix/EASYPLEX", "raw-search.json",
                    "/media/detail/3/default", "raw-movie.json",
                    "/series/show/3556/default", "raw-series.json");

    private HttpServer server;
    private GocineService service;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicInteger authorizedCount = new AtomicInteger();

    private byte[] fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/gocine/" + name)) {
            return in.readAllBytes();
        }
    }

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();

        GocineProperties props = new GocineProperties();
        props.setApiUrl("http://127.0.0.1:" + server.getAddress().getPort());
        props.setToken("token-de-teste");
        GocineClient client = new GocineClient(props, JsonMapper.builder().build());
        service = new GocineService(client, new GocineMapper());
    }

    private void handle(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        if ("Bearer token-de-teste".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            authorizedCount.incrementAndGet();
        }
        String path = exchange.getRequestURI().getPath();
        String name = ROUTES.get(path);
        byte[] body;
        int status;
        if (name == null) {
            // qualquer coisa fora do mapa é 404 — é o que dispara o fallback
            status = 404;
            body = "{\"message\":\"not found\"}".getBytes(StandardCharsets.UTF_8);
        } else {
            status = 200;
            body = fixture(name);
        }
        exchange.getResponseHeaders().add("Content-Type", "application/json");
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
    @DisplayName("home vem parseada de ponta a ponta, com o token no header")
    void homeEndToEnd() {
        var feed = service.getHome();
        assertThat(feed.sections()).isNotEmpty();
        assertThat(feed.featured()).isNotEmpty();
        assertThat(authorizedCount.get()).as("o token foi enviado").isEqualTo(1);
    }

    @Test
    @DisplayName("a segunda chamada sai do cache, sem bater na origem")
    void cachesResponses() {
        service.getHome();
        int afterFirst = requestCount.get();
        service.getHome();
        assertThat(requestCount.get()).as("nenhuma requisição nova").isEqualTo(afterFirst);
    }

    @Test
    @DisplayName("busca com menos de 2 caracteres não chama a origem")
    void shortQueryShortCircuits() {
        assertThat(service.search("a", 30)).isEmpty();
        assertThat(service.search("  ", 30)).isEmpty();
        assertThat(requestCount.get()).isZero();
    }

    @Test
    @DisplayName("busca devolve resultados normalizados")
    void searchEndToEnd() {
        var results = service.search("matrix", 30);
        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(r -> assertThat(r.title()).isNotBlank());
    }

    @Test
    @DisplayName("detalhe de filme inexistente devolve null em vez de estourar")
    void unknownMovieReturnsNull() {
        assertThat(service.getMovieDetail(999999L)).isNull();
    }

    @Test
    @DisplayName("série: tenta /series e cai em /animes quando dá 404")
    void seriesFallsBackToAnime() {
        // 3556 existe como série; pedindo com preferência 'anime', o primeiro
        // request dá 404 e o service precisa tentar o endpoint de séries
        MediaDetail detail = service.getSeriesDetail(3556L, ContentKind.ANIME);
        assertThat(detail).isNotNull();
        assertThat(detail.mediaType()).isEqualTo(MediaType.TV);
        assertThat(detail.contentKind()).isEqualTo(ContentKind.SERIES);
        assertThat(detail.seasons()).isNotEmpty();
        assertThat(requestCount.get()).as("tentou anime e depois série").isEqualTo(2);
    }

    @Test
    @DisplayName("série inexistente nos dois endpoints devolve null")
    void unknownSeriesReturnsNull() {
        assertThat(service.getSeriesDetail(999999L, ContentKind.SERIES)).isNull();
        assertThat(requestCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("stream de episódio resolve pelos números de temporada e episódio")
    void resolvesEpisodeStream() {
        List<MediaVideo> videos =
                service.resolveStreams(3556L, MediaType.TV, ContentKind.SERIES, 1, 1);
        assertThat(videos).isNotEmpty();
        assertThat(videos).allSatisfy(v -> assertThat(v.link()).startsWith("http"));
    }

    @Test
    @DisplayName("stream de filme usa os vídeos do detalhe")
    void resolvesMovieStream() {
        List<MediaVideo> videos = service.resolveStreams(3L, MediaType.MOVIE, null, null, null);
        assertThat(videos).isNotEmpty();
    }
}

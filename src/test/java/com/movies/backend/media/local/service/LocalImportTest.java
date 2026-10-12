package com.movies.backend.media.local.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.HomeFeed;
import com.movies.backend.media.gocine.dto.GocineTypes.HomeSection;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;
import com.movies.backend.media.local.entity.LocalImportItem.Status;
import com.movies.backend.media.local.repository.LocalImportItemRepository;
import com.movies.backend.post.entity.Post;
import com.movies.backend.post.repository.PostRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * A rotina de ponta a ponta: descobre na listagem, importa filme, série e anime
 * (com todos os episódios) de um GoCine falso, e o catálogo local responde sem
 * rede nenhuma.
 */
@SpringBootTest
@ActiveProfiles("test")
class LocalImportTest {

    private static final HttpServer STUB = startStub();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("gocine.web-url", () -> "http://127.0.0.1:" + STUB.getAddress().getPort());
        r.add("gocine.email", () -> "teste@example.com");
        r.add("gocine.password", () -> "senha");
        r.add("gocine.profile-id", () -> "1");
        r.add("local-import.episode-delay-ms", () -> "0");
    }

    @AfterAll
    static void stop() {
        STUB.stop(0);
    }

    @Autowired LocalImporter importer;
    @Autowired LocalCatalogService catalog;
    @Autowired LocalImportItemRepository queue;
    @Autowired PostRepository posts;

    @Test
    void importaEServeDoBanco() {
        // 3 catálogos com 1 página cada + a página vazia que encerra cada um
        for (int i = 0; i < 6; i++) assertThat(importer.discoverOnce()).isTrue();
        assertThat(importer.discoverOnce()).as("tudo descoberto").isFalse();
        assertThat(queue.countByStatus(Status.PENDING)).isEqualTo(3);

        for (int i = 0; i < 3; i++) importer.importNext();
        assertThat(queue.countByStatus(Status.DONE)).isEqualTo(3);

        // anime completo, com a numeração do GoCine
        MediaDetail naruto = catalog.detail(MediaType.TV, ContentKind.ANIME, 683);
        assertThat(naruto.title()).isEqualTo("Naruto");
        assertThat(naruto.seasons()).hasSize(4);
        assertThat(naruto.seasons().stream().mapToInt(s -> s.episodes().size()).sum()).isEqualTo(220);
        assertThat(catalog.streams(MediaType.TV, ContentKind.ANIME, 683, 2, 53)).isNotEmpty();

        assertThat(catalog.detail(MediaType.TV, ContentKind.SERIES, 14173).seasons()).hasSize(1);
        assertThat(catalog.streams(MediaType.MOVIE, null, 33697, null, null)).isNotEmpty();
        assertThat(catalog.search("naru", 30)).extracting("title").containsExactly("Naruto");

        // a nota é a do app, e só ela entra no top 10
        Post review = new Post();
        review.setAuthorId(1L);
        review.setKind("REVIEW");
        review.setMediaType("movie");
        review.setMediaId(33697L);
        review.setRating(9);
        posts.save(review);
        catalog.recordView(MediaType.TV, ContentKind.ANIME, 683, 1L);
        catalog.recordView(MediaType.TV, ContentKind.ANIME, 683, 1L); // mesmo dia não conta de novo

        HomeFeed home = catalog.home();
        assertThat(home.sections()).extracting(HomeSection::title)
                .containsExactly("Bombando", "Lançados recentemente", "Top 10 filmes",
                        "Últimos filmes", "Últimas séries", "Últimos animes");
        assertThat(home.sections().get(0).items()).extracting("title").containsExactly("Naruto");
        assertThat(home.sections().get(2).items().get(0).voteAverage()).isEqualTo(9.0);
        assertThat(catalog.status().episodes()).isEqualTo(221);
    }

    /* --------------------------------------------------------- GoCine falso */

    private static HttpServer startStub() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            s.createContext("/", LocalImportTest::handle);
            s.start();
            return s;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void handle(HttpExchange ex) throws IOException {
        String uri = ex.getRequestURI().toString();
        String body;
        int status = 200;
        if (uri.startsWith("/api/auth/login") || uri.startsWith("/api/profile/select")) {
            body = "{\"ok\":true}";
        } else if (uri.startsWith("/api/bff/genres/")) {
            boolean first = uri.endsWith("page=1");
            body = !first ? "{\"data\":[]}"
                    : uri.contains("latestMovies") ? "{\"data\":[{\"id\":33697,\"title\":\"Sobrevivência\"}]}"
                    : uri.contains("latestseries") ? "{\"data\":[{\"id\":14173,\"name\":\"A Ilha do Tesouro\"}]}"
                    : "{\"data\":[{\"id\":683,\"name\":\"Naruto\"}]}";
        } else if (uri.startsWith("/watch/anime/683")) {
            body = fixture("watch-anime-683-s2e53.html");
        } else if (uri.startsWith("/watch/")) {
            body = fixture("watch-series-14173-s1e1.html");
        } else if (uri.equals("/movie/33697")) {
            body = fixture("movie-33697.html");
        } else if (uri.equals("/series/14173")) {
            body = fixture("series-14173.html");
        } else if (uri.equals("/anime/683")) {
            body = fixture("anime-683.html");
        } else if (uri.equals("/api/bff/series/season/33138")) {
            body = fixture("bff-series-season.json");
        } else if (uri.startsWith("/api/bff/animes/season/")) {
            body = fixture("bff-animes-season-" + uri.substring(uri.lastIndexOf('/') + 1) + ".json");
        } else {
            status = 404;
            body = "{}";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = LocalImportTest.class.getResourceAsStream("/gocine/web/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

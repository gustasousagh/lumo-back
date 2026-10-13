package com.movies.backend.media.local.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.movies.backend.media.local.entity.LocalImportItem.Status;
import com.movies.backend.media.local.repository.LocalImportItemRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Varredura por id: a listagem só mostra o filme 5; os ids 4..1 são testados
 * um a um. Os que não existem viram GONE com uma consulta só — sem gastar as 3
 * tentativas nem ir para "falha".
 */
@SpringBootTest
@ActiveProfiles("test")
class LocalSweepTest {

    private static final HttpServer STUB = startStub();
    private static final List<String> HITS = new CopyOnWriteArrayList<>();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        // banco próprio: o H2 em memória é compartilhado entre contextos da mesma JVM
        r.add("spring.datasource.url", () -> "jdbc:h2:mem:lumo-sweep;DB_CLOSE_DELAY=-1;MODE=MySQL");
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
    @Autowired LocalImportItemRepository queue;

    @Test
    void varreIdsQueAListagemNaoMostra() {
        // listagens (1 página + a vazia de cada) e a varredura dos filmes
        for (int i = 0; i < 10; i++) importer.discoverOnce();
        assertThat(importer.discoverOnce()).as("tudo descoberto").isFalse();
        assertThat(queue.count()).as("filme 5 + ids 4..1").isEqualTo(5);

        while (importer.importNext()) {}

        assertThat(queue.countByStatus(Status.DONE)).isEqualTo(1);
        assertThat(queue.countByStatus(Status.GONE)).isEqualTo(4);
        assertThat(queue.countByStatus(Status.FAILED)).isZero();
        assertThat(HITS.stream().filter(h -> h.equals("/movie/3")).count())
                .as("id inexistente custa uma consulta só").isEqualTo(1);
    }

    private static HttpServer startStub() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            s.createContext("/", LocalSweepTest::handle);
            s.start();
            return s;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void handle(HttpExchange ex) throws IOException {
        String uri = ex.getRequestURI().toString();
        HITS.add(uri);
        String body;
        if (uri.startsWith("/api/auth/login") || uri.startsWith("/api/profile/select")) {
            body = "{\"ok\":true}";
        } else if (uri.startsWith("/api/bff/genres/latestMovies") && uri.endsWith("page=1")) {
            body = "{\"data\":[{\"id\":5,\"title\":\"Sobrevivência\"}]}";
        } else if (uri.startsWith("/api/bff/genres/")) {
            body = "{\"data\":[]}";
        } else if (uri.equals("/movie/5")) {
            body = fixture("movie-33697.html");
        } else if (uri.startsWith("/watch/movie/5")) {
            body = fixture("watch-series-14173-s1e1.html");
        } else {
            body = fixture("not-found.html");
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = LocalSweepTest.class.getResourceAsStream("/gocine/web/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

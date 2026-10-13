package com.movies.backend.media.gocine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.movies.backend.media.gocine.client.GocineWebClient;
import com.movies.backend.media.gocine.config.GocineProperties;
import com.movies.backend.media.gocine.exception.GocineAuthException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sessão do GoCine diante do que aconteceu em 13/10/2026: o site trocou de
 * domínio (redirect 301) e o cookie do domínio antigo não acompanha.
 *
 * "localhost" e "127.0.0.1" são hosts diferentes para cookie, como .app e .zip.
 */
class GocineWebClientTest {

    private HttpServer oldSite;
    private HttpServer newSite;
    private final List<String> newSiteLogins = new CopyOnWriteArrayList<>();
    private final List<String> oldSiteLogins = new CopyOnWriteArrayList<>();
    private int oldLoginStatus = 200;

    @AfterEach
    void stop() {
        if (oldSite != null) oldSite.stop(0);
        if (newSite != null) newSite.stop(0);
    }

    private GocineWebClient client(String base) {
        GocineProperties props = new GocineProperties();
        props.setWebUrl(base);
        props.setEmail("teste@example.com");
        props.setPassword("senha");
        props.setProfileId(1);
        return new GocineWebClient(props, JsonMapper.builder().build());
    }

    private void startSites() throws IOException {
        newSite = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        newSite.createContext("/", ex -> {
            String uri = ex.getRequestURI().toString();
            String cookie = ex.getRequestHeaders().getFirst("Cookie");
            if (uri.startsWith("/api/auth/login")) {
                newSiteLogins.add(uri);
                ex.getResponseHeaders().add("Set-Cookie", "gc_sess=novo; Path=/");
                reply(ex, 200, "{\"ok\":true}");
            } else if (uri.startsWith("/api/profile/select")) {
                reply(ex, 200, "{\"ok\":true}");
            } else if (cookie == null || !cookie.contains("gc_sess=novo")) {
                ex.getResponseHeaders().add("Location", "/login");
                reply(ex, 307, "");
            } else if (uri.equals("/login")) {
                reply(ex, 200, "<html>login</html>");
            } else {
                reply(ex, 200, "<script>self.__next_f.push([1,\"{\\\"rows\\\":[1,2]}\"])</script>");
            }
        });
        newSite.start();

        oldSite = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        oldSite.createContext("/", ex -> {
            String uri = ex.getRequestURI().toString();
            if (uri.startsWith("/api/auth/login")) {
                oldSiteLogins.add(uri);
                if (oldLoginStatus == 429) {
                    ex.getResponseHeaders().add("Retry-After", "300");
                    reply(ex, 429, "{\"ok\":false,\"code\":\"rate_limited\"}");
                    return;
                }
                ex.getResponseHeaders().add("Set-Cookie", "gc_sess=velho; Path=/");
                reply(ex, 200, "{\"ok\":true}");
            } else if (uri.startsWith("/api/profile/select")) {
                reply(ex, 200, "{\"ok\":true}");
            } else {
                ex.getResponseHeaders().add("Location", "http://127.0.0.1:" + newSite.getAddress().getPort() + uri);
                reply(ex, 301, "");
            }
        });
        oldSite.start();
    }

    private static void reply(HttpExchange ex, int status, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, b.length == 0 ? -1 : b.length);
        if (b.length > 0) {
            try (OutputStream out = ex.getResponseBody()) {
                out.write(b);
            }
        }
    }

    @Test
    void segueOSiteQuandoEleMudaDeDominio() throws IOException {
        startSites();
        GocineWebClient c = client("http://localhost:" + oldSite.getAddress().getPort());

        String flight = c.page("/movie/1", false);

        assertThat(flight).contains("\"rows\"");
        assertThat(c.base()).isEqualTo("http://127.0.0.1:" + newSite.getAddress().getPort());
        assertThat(newSiteLogins).hasSize(1);
        // a próxima já vai direto no domínio novo, sem logar de novo
        c.page("/movie/2", false);
        assertThat(newSiteLogins).hasSize(1);
        assertThat(oldSiteLogins).hasSize(1);
    }

    @Test
    void respeitaO429ENaoTentaLogarDeNovo() throws IOException {
        startSites();
        oldLoginStatus = 429;
        GocineWebClient c = client("http://localhost:" + oldSite.getAddress().getPort());

        assertThatThrownBy(() -> c.page("/movie/1", false)).isInstanceOf(GocineAuthException.class);
        assertThatThrownBy(() -> c.page("/movie/1", false)).isInstanceOf(GocineAuthException.class);
        assertThatThrownBy(() -> c.bff("search/x")).isInstanceOf(GocineAuthException.class);

        assertThat(oldSiteLogins).as("bloqueado: não insiste").hasSize(1);
        assertThat(c.base()).as("nada mudou de domínio").contains("localhost");
    }
}

package com.movies.backend.media.gocine.client;

import com.movies.backend.media.gocine.config.GocineProperties;
import com.movies.backend.media.gocine.exception.GocineNotFoundException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Fala com o GoCine novo (web.gocine.app) como um navegador logado.
 *
 * O FLUXO
 *
 *   1. POST /api/auth/login  {identifier, password}  -> cookies gc_sess, gc_device
 *   2. POST /api/profile/select {id}                 -> cookie gc_profile
 *   3. A partir daí as páginas (/, /movie/ID, /series/ID, /watch/...) e o BFF
 *      (/api/bff/...) respondem com os dados, e o link do vídeo sai embutido na
 *      página /watch.
 *
 * A sessão é preguiçosa (loga na primeira necessidade) e se renova sozinha: se
 * uma página responde redirecionando para /login ou /profiles, é sinal de que a
 * sessão caiu — relogamos uma vez e repetimos.
 */
@Component
public class GocineWebClient {

    private static final Logger log = LoggerFactory.getLogger(GocineWebClient.class);

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36";

    private final GocineProperties props;
    private final ObjectMapper om;
    private final HttpClient http;
    private final Cache<String, String> pageCache;

    /** Guarda se a sessão já foi estabelecida; o acesso é serializado em ensureSession. */
    private volatile boolean authenticated = false;
    private final Object authLock = new Object();

    public GocineWebClient(GocineProperties props, ObjectMapper om) {
        this.props = props;
        this.om = om;
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.http =
                HttpClient.newBuilder()
                        .cookieHandler(cookies)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(20))
                        .build();
        this.pageCache =
                Caffeine.newBuilder()
                        .maximumSize(props.getCacheMaxEntries())
                        .expireAfterWrite(Duration.ofMinutes(props.getCacheMinutes()))
                        .build();
    }

    /* ------------------------------------------------------------------ sessão */

    private void login() {
        props.requireCredentials();
        String base = props.normalizedWebUrl();

        // 1) login
        String loginBody;
        try {
            loginBody =
                    om.writeValueAsString(
                            om.createObjectNode()
                                    .put("identifier", props.getEmail())
                                    .put("password", props.getPassword()));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao montar o corpo do login", e);
        }
        JsonNode login = postJson(base + "/api/auth/login", loginBody);
        if (login == null || !login.path("ok").asBoolean(false)) {
            String msg = login == null ? "sem resposta" : login.path("message").asString("");
            throw new IllegalStateException("Login no GoCine falhou: " + msg);
        }

        // 2) seleciona o perfil (o escolhido, ou o primeiro da conta)
        long profileId = props.getProfileId();
        if (profileId <= 0) {
            profileId = firstProfileId(base);
        }
        JsonNode sel =
                postJson(base + "/api/profile/select", "{\"id\":" + profileId + "}");
        if (sel == null || !sel.path("ok").asBoolean(false)) {
            String msg = sel == null ? "sem resposta" : sel.path("message").asString("");
            throw new IllegalStateException("Seleção de perfil falhou: " + msg);
        }
        log.info("Sessão GoCine estabelecida (perfil {}).", profileId);
    }

    /** Primeiro perfil da conta, quando GOCINE_PROFILE_ID não foi definido. */
    private long firstProfileId(String base) {
        JsonNode user = getJsonRaw(base + "/api/bff/user");
        JsonNode profiles = user == null ? null : user.get("profiles");
        if (profiles != null && profiles.isArray() && !profiles.isEmpty()) {
            return profiles.get(0).path("id").asLong(0);
        }
        throw new IllegalStateException("Nenhum perfil encontrado na conta GoCine");
    }

    private void ensureSession() {
        if (authenticated) return;
        synchronized (authLock) {
            if (authenticated) return;
            login();
            authenticated = true;
        }
    }

    /** Força novo login na próxima chamada (sessão caiu). */
    private void invalidate() {
        synchronized (authLock) {
            authenticated = false;
        }
    }

    /* -------------------------------------------------------------- requisições */

    /**
     * Busca uma página e devolve o fluxo RSC já reconstruído. Se a sessão caiu
     * (redirecionou para /login ou /profiles), reloga uma vez e repete.
     *
     * @param useCache false para /watch, cujo link de vídeo tem token de vida
     *     curta e não pode ser servido de cache.
     */
    public String page(String path, boolean useCache) {
        if (useCache) {
            String hit = pageCache.getIfPresent(path);
            if (hit != null) return hit;
        }
        ensureSession();
        String flight = fetchPage(path);
        if (flight == null) {
            // sessão provavelmente caiu: reloga e tenta de novo, uma vez
            invalidate();
            ensureSession();
            flight = fetchPage(path);
            if (flight == null) {
                throw new IllegalStateException("GoCine exigiu login mesmo após relogar: " + path);
            }
        }
        if (useCache) pageCache.put(path, flight);
        return flight;
    }

    /** null quando a resposta redirecionou para login/perfil (sessão inválida). */
    private String fetchPage(String path) {
        String base = props.normalizedWebUrl();
        HttpResponse<String> res = send(base + path, null, "text/html");
        String finalPath = res.uri() == null ? "" : res.uri().getPath();
        if (finalPath.startsWith("/login") || finalPath.startsWith("/profiles")) {
            return null;
        }
        return Rsc.flight(om, res.body());
    }

    /** GET num endpoint BFF (/api/bff/...), com JSON de volta. Reloga em 401. */
    public JsonNode bff(String path) {
        ensureSession();
        String base = props.normalizedWebUrl();
        String url = base + "/api/bff/" + path;
        HttpResponse<String> res = send(url, null, "application/json");
        if (res.statusCode() == 401) {
            invalidate();
            ensureSession();
            res = send(url, null, "application/json");
        }
        if (res.statusCode() == 404) {
            throw new GocineNotFoundException(res.body());
        }
        if (res.statusCode() >= 400) {
            throw new IllegalStateException("BFF " + path + " respondeu " + res.statusCode());
        }
        return parse(res.body());
    }

    private JsonNode getJsonRaw(String url) {
        HttpResponse<String> res = send(url, null, "application/json");
        return res.statusCode() >= 400 ? null : parse(res.body());
    }

    private JsonNode postJson(String url, String body) {
        HttpResponse<String> res = send(url, body, "application/json");
        if (res.statusCode() >= 400) {
            log.warn("POST {} respondeu {}", url, res.statusCode());
            return null;
        }
        return parse(res.body());
    }

    /* ------------------------------------------------------------------ baixo nível */

    private HttpResponse<String> send(String url, String jsonBody, String accept) {
        String base = props.normalizedWebUrl();
        HttpRequest.Builder b =
                HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", accept)
                        .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                        .header("Referer", base + "/")
                        .header("Origin", base);
        if (jsonBody != null) {
            b.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        } else {
            b.GET();
        }
        try {
            return http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chamada ao GoCine interrompida (" + url + ")", e);
        } catch (Exception e) {
            // Só InterruptedException marca a thread: marcar em erro de rede comum
            // deixava a thread do Tomcat "interrompida" e a próxima chamada nela
            // falhava sem nem sair da máquina.
            throw new IllegalStateException("Falha ao chamar o GoCine (" + url + "): " + e.getMessage(), e);
        }
    }

    private JsonNode parse(String body) {
        try {
            return om.readTree(body == null ? "null" : body);
        } catch (Exception e) {
            return null;
        }
    }

    /* ------------------------------------------------------- recortes do RSC */

    public JsonNode pageArray(String flight, String key) {
        return Rsc.array(om, flight, key);
    }

    public JsonNode pageObjectContaining(String flight, String anchor) {
        return Rsc.objectContaining(om, flight, anchor);
    }

    public String pageString(String flight, String key) {
        return Rsc.firstString(flight, key);
    }

    public JsonNode pageArrayBefore(String flight, String anchor, String key) {
        return Rsc.arrayBefore(om, flight, anchor, key);
    }

    public String pageMatch(String flight, java.util.regex.Pattern p) {
        return Rsc.firstMatch(flight, p);
    }
}

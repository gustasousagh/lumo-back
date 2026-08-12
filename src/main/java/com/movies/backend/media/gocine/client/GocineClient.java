package com.movies.backend.media.gocine.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.movies.backend.media.gocine.config.GocineProperties;
import com.movies.backend.media.gocine.exception.GocineNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Fala com a API do GoCine.
 *
 * O token vai em dois lugares (header Authorization e query param `user`) porque
 * a API do GoCine exige os dois — foi assim que a camada TypeScript funcionava.
 *
 * As respostas ficam num cache em memória com TTL curto, substituindo o
 * `next: { revalidate: 300 }` que o Next fazia. O catálogo muda pouco e a home
 * puxa um payload de ~54 KB, então sem cache cada visita vira uma ida à origem.
 */
@Component
public class GocineClient {

    private static final Logger log = LoggerFactory.getLogger(GocineClient.class);

    private final GocineProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Cache<String, JsonNode> cache;

    public GocineClient(GocineProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
        this.cache =
                Caffeine.newBuilder()
                        .maximumSize(properties.getCacheMaxEntries())
                        .expireAfterWrite(Duration.ofMinutes(properties.getCacheMinutes()))
                        .build();
    }

    /**
     * GET no GoCine, com cache. O path já vem montado pelo service.
     *
     * @throws GocineNotFoundException quando a origem devolve 404
     */
    public JsonNode get(String path) {
        JsonNode cached = cache.getIfPresent(path);
        if (cached != null) return cached;

        String token = properties.requireToken();
        String normalized = path.startsWith("/") ? path : "/" + path;
        String separator = normalized.contains("?") ? "&" : "?";
        String url =
                properties.normalizedBaseUrl()
                        + normalized
                        + separator
                        + "user="
                        + URLEncoder.encode(token, StandardCharsets.UTF_8);

        try {
            String body =
                    restClient
                            .get()
                            .uri(java.net.URI.create(url))
                            .header("Authorization", "Bearer " + token)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .body(String.class);
            JsonNode parsed = objectMapper.readTree(body == null ? "null" : body);
            cache.put(path, parsed);
            return parsed;
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new GocineNotFoundException(body);
            }
            log.warn("GoCine {} respondeu {}", path, e.getStatusCode());
            throw new IllegalStateException(
                    "GoCine " + e.getStatusCode().value() + ": " + truncate(body), e);
        } catch (GocineNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Falha ao chamar o GoCine em {}: {}", path, e.getMessage());
            throw new IllegalStateException("Falha ao chamar o GoCine: " + e.getMessage(), e);
        }
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() <= 200 ? s : s.substring(0, 200);
    }
}

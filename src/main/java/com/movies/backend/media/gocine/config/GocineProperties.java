package com.movies.backend.media.gocine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciais do GoCine. Vêm de GOCINE_API_URL e GOCINE_JWT_SECRET — este é o
 * único lugar da stack que conhece o token (antes ficava no servidor do Next).
 */
@ConfigurationProperties(prefix = "gocine")
public class GocineProperties {

    /** Base da API, sem barra no fim. Ex.: https://siteapi.gocine.app/api */
    private String apiUrl = "";

    /** Token de acesso. Vai no header Authorization e no query param `user`. */
    private String token = "";

    /** Por quantos minutos guardar as respostas em memória. */
    private int cacheMinutes = 5;

    /** Teto de entradas no cache (busca é ilimitada por natureza). */
    private int cacheMaxEntries = 500;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getCacheMinutes() {
        return cacheMinutes;
    }

    public void setCacheMinutes(int cacheMinutes) {
        this.cacheMinutes = cacheMinutes;
    }

    public int getCacheMaxEntries() {
        return cacheMaxEntries;
    }

    public void setCacheMaxEntries(int cacheMaxEntries) {
        this.cacheMaxEntries = cacheMaxEntries;
    }

    /** Base normalizada (sem barras sobrando no fim). */
    public String normalizedBaseUrl() {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException("GOCINE_API_URL não configurada");
        }
        return apiUrl.replaceAll("/+$", "");
    }

    public String requireToken() {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("GOCINE_JWT_SECRET não configurada");
        }
        return token;
    }
}

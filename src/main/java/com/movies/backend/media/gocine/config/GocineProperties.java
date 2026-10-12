package com.movies.backend.media.gocine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciais e endereços do GoCine.
 *
 * O GoCine trocou de backend: o antigo `siteapi` (token fixo na URL) saiu do ar
 * e o catálogo passou a viver atrás de uma sessão autenticada em
 * `web.gocine.app` — login por e-mail/senha, perfil selecionado, cookie de
 * sessão. Por isso aqui agora moram as credenciais da conta, e não mais um
 * token avulso.
 */
@ConfigurationProperties(prefix = "gocine")
public class GocineProperties {

    /** Base do site novo, sem barra no fim. Ex.: https://web.gocine.app */
    private String webUrl = "https://web.gocine.app";

    /** E-mail da conta GoCine usada para logar. */
    private String email = "";

    /** Senha da conta GoCine. */
    private String password = "";

    /**
     * Id do perfil a selecionar depois do login. Zero ou negativo = pega o
     * primeiro perfil da conta automaticamente.
     */
    private long profileId = 0;

    /** Por quantos minutos guardar as páginas de catálogo em memória. */
    private int cacheMinutes = 5;

    /** Teto de entradas no cache. */
    private int cacheMaxEntries = 500;

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getProfileId() {
        return profileId;
    }

    public void setProfileId(long profileId) {
        this.profileId = profileId;
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
    public String normalizedWebUrl() {
        if (webUrl == null || webUrl.isBlank()) {
            throw new IllegalStateException("GOCINE_WEB_URL não configurada");
        }
        return webUrl.replaceAll("/+$", "");
    }

    public void requireCredentials() {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("GOCINE_EMAIL/GOCINE_PASSWORD não configurados");
        }
    }
}

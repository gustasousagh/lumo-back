package com.movies.backend.media.local.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ritmo da importação do GoCine para o banco local.
 *
 * Tudo é sequencial, uma requisição de cada vez: a ideia é migrar devagar sem
 * pesar no site de origem.
 */
@ConfigurationProperties(prefix = "local-import")
public class LocalImportProperties {

    /** Liga a rotina. Desligada nos testes. */
    private boolean enabled = true;

    /** Intervalo entre um título (ou uma página da listagem) e o próximo. */
    private long tickMs = 60_000;

    /** Espera depois do arranque antes da primeira rodada. */
    private long initialDelayMs = 60_000;

    /** Pausa entre os episódios de uma mesma série (cada um é uma requisição). */
    private long episodeDelayMs = 2_000;

    /** Depois de varrer a listagem inteira, de quanto em quanto tempo procurar títulos novos. */
    private int refreshHours = 6;

    /** Tentativas antes de desistir de um título. */
    private int maxAttempts = 3;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getTickMs() { return tickMs; }
    public void setTickMs(long tickMs) { this.tickMs = tickMs; }
    public long getInitialDelayMs() { return initialDelayMs; }
    public void setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = initialDelayMs; }
    public long getEpisodeDelayMs() { return episodeDelayMs; }
    public void setEpisodeDelayMs(long episodeDelayMs) { this.episodeDelayMs = episodeDelayMs; }
    public int getRefreshHours() { return refreshHours; }
    public void setRefreshHours(int refreshHours) { this.refreshHours = refreshHours; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
}

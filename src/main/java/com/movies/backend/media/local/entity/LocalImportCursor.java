package com.movies.backend.media.local.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Até que página da listagem do GoCine a descoberta já foi, por catálogo.
 * Fica no banco para o Pi poder reiniciar sem recomeçar do zero.
 */
@Entity
@Table(name = "local_import_cursors")
public class LocalImportCursor {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private LocalKind kind;

    @Column(nullable = false)
    private int nextPage = 1;

    /** Chegou ao fim da listagem; só volta a olhar em refreshAt. */
    @Column(nullable = false)
    private boolean exhausted = false;

    /**
     * Passada de atualização (recomeçou da página 1 atrás de títulos novos):
     * para na primeira página sem nada novo, em vez de varrer tudo de novo.
     */
    @Column(nullable = false)
    private boolean refreshing = false;

    private Instant refreshAt;

    @Column(nullable = false)
    private Instant lastRunAt = Instant.EPOCH;

    public LocalImportCursor() {}

    public LocalImportCursor(LocalKind kind) {
        this.kind = kind;
    }

    public LocalKind getKind() { return kind; }
    public int getNextPage() { return nextPage; }
    public void setNextPage(int nextPage) { this.nextPage = nextPage; }
    public boolean isExhausted() { return exhausted; }
    public void setExhausted(boolean exhausted) { this.exhausted = exhausted; }
    public boolean isRefreshing() { return refreshing; }
    public void setRefreshing(boolean refreshing) { this.refreshing = refreshing; }
    public Instant getRefreshAt() { return refreshAt; }
    public void setRefreshAt(Instant refreshAt) { this.refreshAt = refreshAt; }
    public Instant getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(Instant lastRunAt) { this.lastRunAt = lastRunAt; }
}

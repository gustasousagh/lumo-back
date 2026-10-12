package com.movies.backend.media.local.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Linha única com o liga/desliga da rotina, para a pausa sobreviver a reinício. */
@Entity
@Table(name = "local_import_state")
public class LocalImportState {

    public static final long SINGLETON = 1L;

    @Id
    private Long id = SINGLETON;

    @Column(nullable = false)
    private boolean paused = false;

    public Long getId() { return id; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
}

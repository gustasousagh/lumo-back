package com.movies.backend.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Faixa dentro de uma playlist, na posição em que aparece.
 *
 * <p>A mesma faixa pode entrar duas vezes de propósito (repetir no meio do set),
 * por isso não existe restrição de unicidade em (playlist, track) — a chave da
 * ordem é {@code position}, reescrita inteira a cada reordenação.
 */
@Entity
@Table(name = "library_playlist_tracks", indexes = {
        @Index(name = "idx_pltrack_playlist", columnList = "playlistId,position")
})
public class PlaylistTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long playlistId;

    @Column(nullable = false)
    private Long trackId;

    /** Posição na fila, começando em 0. */
    @Column(nullable = false)
    private int position;

    /** Quem colocou — em playlist colaborativa isso aparece na tela. */
    @Column(nullable = false)
    private Long addedById;

    @Column(nullable = false, updatable = false)
    private Instant addedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(Long playlistId) {
        this.playlistId = playlistId;
    }

    public Long getTrackId() {
        return trackId;
    }

    public void setTrackId(Long trackId) {
        this.trackId = trackId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Long getAddedById() {
        return addedById;
    }

    public void setAddedById(Long addedById) {
        this.addedById = addedById;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }
}

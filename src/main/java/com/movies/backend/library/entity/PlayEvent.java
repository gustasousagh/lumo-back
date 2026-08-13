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
 * Registro de "tocou uma faixa". O contador em {@link Track#getPlayCount()} dá o
 * total; esta tabela dá a linha do tempo, que é o que o gráfico do painel admin
 * precisa (um contador não sabe dizer "quantas nos últimos 30 dias").
 *
 * <p>O front só registra depois de alguns segundos de reprodução — pular a faixa
 * na hora não conta como play.
 */
@Entity
@Table(name = "library_play_events", indexes = {
        @Index(name = "idx_play_track", columnList = "trackId"),
        @Index(name = "idx_play_at", columnList = "playedAt")
})
public class PlayEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long trackId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private Instant playedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTrackId() {
        return trackId;
    }

    public void setTrackId(Long trackId) {
        this.trackId = trackId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Instant playedAt) {
        this.playedAt = playedAt;
    }
}

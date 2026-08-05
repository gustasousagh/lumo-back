package com.movies.backend.progress.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Progresso de reprodução ("continue assistindo") de um usuário em uma mídia.
 * Como temporada/episódio são opcionais, usamos uma contentKey
 * ("mediaType:mediaId:season:episode", com 0 quando nulo) para garantir
 * unicidade por (userId, contentKey) e permitir upsert.
 */
@Entity
@Table(name = "watch_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_progress_user_key", columnNames = {"userId", "contentKey"}),
        indexes = {
                @Index(name = "idx_progress_user", columnList = "userId")
        })
public class WatchProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String mediaType;

    @Column(nullable = false)
    private Long mediaId;

    private String contentKind;

    @Column(nullable = false)
    private String title;

    private String posterUrl;

    private Integer seasonNumber;

    private Integer episodeNumber;

    /** Chave única do conteúdo: mediaType:mediaId:season:episode (0 quando nulo). */
    @Column(nullable = false)
    private String contentKey;

    @Column(nullable = false, columnDefinition = "double default 0")
    private double progressSec = 0;

    private Double durationSec;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public String getContentKind() {
        return contentKind;
    }

    public void setContentKind(String contentKind) {
        this.contentKind = contentKind;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public Integer getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(Integer seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public String getContentKey() {
        return contentKey;
    }

    public void setContentKey(String contentKey) {
        this.contentKey = contentKey;
    }

    public double getProgressSec() {
        return progressSec;
    }

    public void setProgressSec(double progressSec) {
        this.progressSec = progressSec;
    }

    public Double getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Double durationSec) {
        this.durationSec = durationSec;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

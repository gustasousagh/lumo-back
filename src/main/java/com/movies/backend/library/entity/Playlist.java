package com.movies.backend.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Playlist de músicas.
 *
 * <p>O {@code shareCode} é gerado sempre, mesmo em playlist privada: assim
 * "compartilhar" é só trocar a visibilidade, sem precisar inventar o código na
 * hora e sem que o link mude toda vez que a pessoa liga e desliga o
 * compartilhamento.
 *
 * <p>{@code collaborative} vale junto com a lista de {@link PlaylistCollaborator}:
 * quem está na lista pode adicionar, remover e reordenar faixas. Renomear e
 * apagar continuam sendo só do dono.
 */
@Entity
@Table(name = "library_playlists", indexes = {
        @Index(name = "idx_playlist_owner", columnList = "ownerId"),
        @Index(name = "idx_playlist_share", columnList = "shareCode", unique = true)
})
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String description;

    /** Capa escolhida à mão; quando nula, o front monta o mosaico das capas das faixas. */
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaylistVisibility visibility = PlaylistVisibility.PRIVATE;

    @Column(nullable = false, unique = true, length = 12)
    private String shareCode;

    @Column(nullable = false)
    private boolean collaborative = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public PlaylistVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(PlaylistVisibility visibility) {
        this.visibility = visibility;
    }

    public String getShareCode() {
        return shareCode;
    }

    public void setShareCode(String shareCode) {
        this.shareCode = shareCode;
    }

    public boolean isCollaborative() {
        return collaborative;
    }

    public void setCollaborative(boolean collaborative) {
        this.collaborative = collaborative;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

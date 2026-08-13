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
 * Curtida em faixa ou livro. Uma tabela só para os dois, com {@code kind}
 * dizendo qual — são exatamente os mesmos campos, e duas tabelas idênticas
 * significariam duplicar cada consulta de favoritos.
 */
@Entity
@Table(name = "library_favorites", indexes = {
        @Index(name = "idx_fav_user", columnList = "userId,kind"),
        @Index(name = "idx_fav_ref", columnList = "kind,refId")
})
public class LibraryFavorite {

    /** Tipo do item favoritado. */
    public enum Kind {
        TRACK,
        BOOK
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String kind;

    @Column(nullable = false)
    private Long refId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind.name();
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

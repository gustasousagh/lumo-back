package com.movies.backend.media.local.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** Fila de importação: um título descoberto no catálogo do GoCine. */
@Entity
@Table(
        name = "local_import_queue",
        uniqueConstraints = @UniqueConstraint(name = "uk_local_queue_source", columnNames = {"kind", "sourceId"}),
        indexes = @Index(name = "idx_local_queue_status", columnList = "status, id"))
public class LocalImportItem {

    public enum Status {
        PENDING,
        DONE,
        /** Falhou mais vezes que o limite; fica de fora até alguém reenfileirar. */
        FAILED,
        /** O GoCine não conhece mais o id. */
        GONE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LocalKind kind;

    @Column(nullable = false)
    private Long sourceId;

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(length = 500)
    private String lastError;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public LocalKind getKind() { return kind; }
    public void setKind(LocalKind kind) { this.kind = kind; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

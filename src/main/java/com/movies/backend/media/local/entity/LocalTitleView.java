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
import java.time.LocalDate;

/**
 * "Fulano abriu este título neste dia" — no máximo uma linha por pessoa/título/dia.
 * É o que ordena o "Bombando": pessoas distintas, não cliques repetidos.
 */
@Entity
@Table(
        name = "local_title_views",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_local_view", columnNames = {"kind", "sourceId", "userId", "viewDay"}),
        indexes = @Index(name = "idx_local_view_day", columnList = "viewDay"))
public class LocalTitleView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LocalKind kind;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate viewDay;

    public LocalTitleView() {}

    public LocalTitleView(LocalKind kind, Long sourceId, Long userId, LocalDate viewDay) {
        this.kind = kind;
        this.sourceId = sourceId;
        this.userId = userId;
        this.viewDay = viewDay;
    }

    public Long getId() { return id; }
    public LocalKind getKind() { return kind; }
    public Long getSourceId() { return sourceId; }
    public Long getUserId() { return userId; }
    public LocalDate getViewDay() { return viewDay; }
}

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

/**
 * Filme, série ou anime importado do GoCine para o banco local.
 *
 * O id público é o do GoCine (sourceId) junto com o kind: assim progresso,
 * listas e resenhas, que guardam esse id, continuam valendo nos dois modos.
 *
 * A nota NÃO é guardada: no modo local a nota é a média das resenhas do app.
 */
@Entity
@Table(
        name = "local_titles",
        uniqueConstraints = @UniqueConstraint(name = "uk_local_title_source", columnNames = {"kind", "sourceId"}),
        indexes = {
            @Index(name = "idx_local_title_imported", columnList = "importedAt"),
            @Index(name = "idx_local_title_name", columnList = "title")
        })
public class LocalTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LocalKind kind;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(length = 500)
    private String posterUrl;

    @Column(length = 500)
    private String backdropUrl;

    private String releaseDate;

    private String genre;

    /** Gêneros separados por vírgula. */
    @Column(length = 500)
    private String genres;

    private Integer runtimeMinutes;

    @Column(length = 500)
    private String trailerUrl;

    /** Elenco como JSON ([{name, character, photoUrl}]). */
    @Column(columnDefinition = "TEXT")
    private String castJson;

    /** Players do filme como JSON ([{server, link, lang, hd}]). Série guarda por episódio. */
    @Column(columnDefinition = "TEXT")
    private String streamsJson;

    @Column(nullable = false)
    private Instant importedAt = Instant.now();

    public Long getId() { return id; }
    public LocalKind getKind() { return kind; }
    public void setKind(LocalKind kind) { this.kind = kind; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getBackdropUrl() { return backdropUrl; }
    public void setBackdropUrl(String backdropUrl) { this.backdropUrl = backdropUrl; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }
    public Integer getRuntimeMinutes() { return runtimeMinutes; }
    public void setRuntimeMinutes(Integer runtimeMinutes) { this.runtimeMinutes = runtimeMinutes; }
    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }
    public String getCastJson() { return castJson; }
    public void setCastJson(String castJson) { this.castJson = castJson; }
    public String getStreamsJson() { return streamsJson; }
    public void setStreamsJson(String streamsJson) { this.streamsJson = streamsJson; }
    public Instant getImportedAt() { return importedAt; }
    public void setImportedAt(Instant importedAt) { this.importedAt = importedAt; }
}

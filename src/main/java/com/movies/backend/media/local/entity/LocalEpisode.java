package com.movies.backend.media.local.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Episódio de série/anime local. A temporada mora aqui mesmo (número + nome),
 * sem tabela própria — o app nunca lê uma temporada sem os episódios.
 *
 * episodeNumber é o do GoCine: em anime é absoluto (T2 de Naruto começa no 53).
 */
@Entity
@Table(
        name = "local_episodes",
        indexes = @Index(name = "idx_local_ep_title", columnList = "titleId, seasonNumber, episodeNumber"))
public class LocalEpisode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long titleId;

    @Column(nullable = false)
    private int seasonNumber;

    private String seasonName;

    @Column(nullable = false)
    private int episodeNumber;

    /** Id do episódio no GoCine, também usado como id público do episódio. */
    private Long sourceEpisodeId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(length = 500)
    private String stillUrl;

    private Integer runtimeMinutes;

    /** Players como JSON ([{server, link, lang, hd}]); "[]" se o GoCine não tinha. */
    @Column(columnDefinition = "TEXT")
    private String streamsJson;

    public Long getId() { return id; }
    public Long getTitleId() { return titleId; }
    public void setTitleId(Long titleId) { this.titleId = titleId; }
    public int getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(int seasonNumber) { this.seasonNumber = seasonNumber; }
    public String getSeasonName() { return seasonName; }
    public void setSeasonName(String seasonName) { this.seasonName = seasonName; }
    public int getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(int episodeNumber) { this.episodeNumber = episodeNumber; }
    public Long getSourceEpisodeId() { return sourceEpisodeId; }
    public void setSourceEpisodeId(Long sourceEpisodeId) { this.sourceEpisodeId = sourceEpisodeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getStillUrl() { return stillUrl; }
    public void setStillUrl(String stillUrl) { this.stillUrl = stillUrl; }
    public Integer getRuntimeMinutes() { return runtimeMinutes; }
    public void setRuntimeMinutes(Integer runtimeMinutes) { this.runtimeMinutes = runtimeMinutes; }
    public String getStreamsJson() { return streamsJson; }
    public void setStreamsJson(String streamsJson) { this.streamsJson = streamsJson; }
}

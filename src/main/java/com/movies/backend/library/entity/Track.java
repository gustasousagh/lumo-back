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
 * Uma faixa de música enviada para a biblioteca.
 *
 * <p>Quase todo campo aqui sai das tags ID3 do MP3 enviado — quem preenche é o
 * {@code AudioMetadataExtractor}. O admin pode corrigir depois pelo painel, então
 * nada disso é obrigatório: MP3 sem tag nenhuma entra com o nome do arquivo como
 * título, em vez de ser rejeitado.
 *
 * <p>{@code storageKey} é o nome do arquivo dentro de ./data/library/music.
 * {@code checksum} é o SHA-256 do conteúdo e existe para barrar upload repetido:
 * o mesmo MP3 mandado duas vezes não vira duas faixas.
 */
@Entity
@Table(name = "library_tracks", indexes = {
        @Index(name = "idx_track_checksum", columnList = "checksum"),
        @Index(name = "idx_track_artist", columnList = "artist"),
        @Index(name = "idx_track_album", columnList = "album")
})
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 200)
    private String artist;

    @Column(length = 200)
    private String album;

    /** Artista do álbum — difere de {@code artist} em coletâneas e participações. */
    @Column(length = 200)
    private String albumArtist;

    @Column(length = 100)
    private String genre;

    /**
     * Ano de lançamento. A coluna se chama release_year porque "year" é
     * palavra reservada em SQL — com o nome padrão, o CREATE TABLE falha.
     */
    @Column(name = "release_year")
    private Integer year;

    private Integer trackNumber;

    private Integer discNumber;

    /** Duração em segundos, lida do próprio arquivo. */
    private Integer durationSec;

    /** Bitrate em kbps, só informativo (aparece no painel admin). */
    private Integer bitrateKbps;

    @Column(nullable = false)
    private String storageKey;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 100)
    private String mimeType = "audio/mpeg";

    /** Nome original do arquivo, preservado para o download sair legível. */
    @Column(nullable = false, length = 300)
    private String originalFilename;

    /** URL da capa (extraída do ID3 ou definida pelo admin). */
    private String coverUrl;

    @Column(nullable = false)
    private Long uploadedById;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Quantas vezes a faixa foi tocada — alimenta "mais tocadas" no painel. */
    @Column(nullable = false)
    private long playCount = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    public Integer getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Integer discNumber) {
        this.discNumber = discNumber;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public Integer getBitrateKbps() {
        return bitrateKbps;
    }

    public void setBitrateKbps(Integer bitrateKbps) {
        this.bitrateKbps = bitrateKbps;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Long getUploadedById() {
        return uploadedById;
    }

    public void setUploadedById(Long uploadedById) {
        this.uploadedById = uploadedById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(long playCount) {
        this.playCount = playCount;
    }
}

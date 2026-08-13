package com.movies.backend.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Um livro em PDF enviado para a biblioteca.
 *
 * <p>Título, autor, assunto e número de páginas saem do dicionário de metadados
 * do próprio PDF ({@code BookMetadataExtractor}). A capa é a primeira página
 * renderizada como JPEG — PDF não tem campo de capa, e a primeira página é a
 * capa em praticamente todo livro digitalizado ou diagramado.
 */
@Entity
@Table(name = "library_books", indexes = {
        @Index(name = "idx_book_checksum", columnList = "checksum"),
        @Index(name = "idx_book_author", columnList = "author")
})
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 200)
    private String author;

    @Column(length = 200)
    private String publisher;

    @Column(length = 100)
    private String genre;

    @Column(length = 10)
    private String language;

    /**
     * Ano de lançamento. A coluna se chama release_year porque "year" é
     * palavra reservada em SQL — com o nome padrão, o CREATE TABLE falha.
     */
    @Column(name = "release_year")
    private Integer year;

    private Integer pageCount;

    @Column(length = 20)
    private String isbn;

    @Lob
    private String description;

    @Column(nullable = false)
    private String storageKey;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 100)
    private String mimeType = "application/pdf";

    @Column(nullable = false, length = 300)
    private String originalFilename;

    private String coverUrl;

    @Column(nullable = false)
    private Long uploadedById;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Quantas vezes o livro foi aberto no leitor. */
    @Column(nullable = false)
    private long readCount = 0;

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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public long getReadCount() {
        return readCount;
    }

    public void setReadCount(long readCount) {
        this.readCount = readCount;
    }
}

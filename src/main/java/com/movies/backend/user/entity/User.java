package com.movies.backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entity = como o usuário é guardado no banco (tabela "users").
 * É a camada mais "de baixo": ninguém da web fala direto com ela,
 * quem manipula é o Repository (persistência) e o Service (regras).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /** Senha já criptografada (BCrypt). Nunca guardamos a senha pura. */
    @Column(nullable = false)
    private String password;

    /** Só vira true depois que o usuário confirma o email. */
    @Column(nullable = false)
    private boolean enabled = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // ------------------------------------------------------------------ PERFIL
    /** Apelido único (ex.: "@joao"). Definido depois do cadastro, por isso nullable. */
    @Column(unique = true)
    private String username;

    /** Pequena descrição que o usuário escreve sobre si. */
    @Column(length = 500)
    private String bio;

    /** URL da foto de perfil. */
    private String avatarUrl;

    /** URL da imagem de capa do perfil. */
    private String coverUrl;

    /** Chave da paleta de cores escolhida pelo usuário. */
    @Column(nullable = false, columnDefinition = "varchar(255) default 'sunset'")
    private String accent = "sunset";

    /** Papel do usuário no sistema. Guardado como texto minúsculo ("user"/"admin"). */
    @Column(nullable = false, columnDefinition = "varchar(255) default 'user'")
    private String role = Role.USER.dbValue();

    /**
     * Conta suspensa pelo admin. Diferente de {@code enabled}, que é sobre
     * confirmar o email: aqui o email está confirmado, mas o acesso foi cortado.
     */
    @Column(nullable = false)
    private boolean suspended = false;

    // ------------------------------------------------------- "ASSISTINDO AGORA"
    private String watchingTitle;
    private String watchingPosterUrl;
    private Long watchingMediaId;
    private String watchingMediaType;
    private Instant watchingUpdatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getAccent() {
        return accent;
    }

    public void setAccent(String accent) {
        this.accent = accent;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /** Papel tipado (o banco guarda texto; aqui a leitura já vem validada). */
    public Role role() {
        return Role.from(role);
    }

    public void setRole(Role role) {
        this.role = role.dbValue();
    }

    public boolean isAdmin() {
        return role() == Role.ADMIN;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public String getWatchingTitle() {
        return watchingTitle;
    }

    public void setWatchingTitle(String watchingTitle) {
        this.watchingTitle = watchingTitle;
    }

    public String getWatchingPosterUrl() {
        return watchingPosterUrl;
    }

    public void setWatchingPosterUrl(String watchingPosterUrl) {
        this.watchingPosterUrl = watchingPosterUrl;
    }

    public Long getWatchingMediaId() {
        return watchingMediaId;
    }

    public void setWatchingMediaId(Long watchingMediaId) {
        this.watchingMediaId = watchingMediaId;
    }

    public String getWatchingMediaType() {
        return watchingMediaType;
    }

    public void setWatchingMediaType(String watchingMediaType) {
        this.watchingMediaType = watchingMediaType;
    }

    public Instant getWatchingUpdatedAt() {
        return watchingUpdatedAt;
    }

    public void setWatchingUpdatedAt(Instant watchingUpdatedAt) {
        this.watchingUpdatedAt = watchingUpdatedAt;
    }
}

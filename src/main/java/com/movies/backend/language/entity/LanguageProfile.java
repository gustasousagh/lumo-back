package com.movies.backend.language.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * O progresso de uma pessoa no curso de idiomas.
 *
 * Guarda só o que é DELA. O conteúdo do curso (palavras, lições, exercícios)
 * não mora no banco: é um arquivo estático servido pelo front, imutável e
 * versionado junto com ele. Duplicar 8.515 exercícios aqui seria carregar o
 * Raspberry para ler sempre a mesma coisa.
 *
 * `lastStudyDate` é LocalDate e não Instant de propósito: a ofensiva vira no
 * dia do usuário, não em UTC. Quem estuda às 23h de sábado não pode perder a
 * sequência porque em Londres já é domingo.
 */
@Entity
@Table(name = "language_profile", indexes = {
        @Index(name = "idx_langprofile_user", columnList = "userId", unique = true)
})
public class LanguageProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private long xpTotal = 0;

    /** XP feito hoje. Zera na primeira leitura de um dia novo. */
    @Column(nullable = false)
    private int todayXp = 0;

    @Column(nullable = false)
    private int dailyGoalXp = 20;

    @Column(nullable = false)
    private int streakDays = 0;

    @Column(nullable = false)
    private int longestStreak = 0;

    /**
     * Congelamentos disponíveis. Um dia pulado consome um em vez de zerar a
     * ofensiva — perder sessenta dias por um dia corrido é o que mais faz
     * gente largar app de idioma.
     */
    @Column(nullable = false)
    private int freezesAvailable = 1;

    /** Aproximação: soma de acertos, limitada ao tamanho do acervo. */
    @Column(nullable = false)
    private int wordsMastered = 0;

    private LocalDate lastStudyDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public long getXpTotal() { return xpTotal; }
    public void setXpTotal(long xpTotal) { this.xpTotal = xpTotal; }
    public int getTodayXp() { return todayXp; }
    public void setTodayXp(int todayXp) { this.todayXp = todayXp; }
    public int getDailyGoalXp() { return dailyGoalXp; }
    public void setDailyGoalXp(int dailyGoalXp) { this.dailyGoalXp = dailyGoalXp; }
    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
    public int getFreezesAvailable() { return freezesAvailable; }
    public void setFreezesAvailable(int freezesAvailable) { this.freezesAvailable = freezesAvailable; }
    public int getWordsMastered() { return wordsMastered; }
    public void setWordsMastered(int wordsMastered) { this.wordsMastered = wordsMastered; }
    public LocalDate getLastStudyDate() { return lastStudyDate; }
    public void setLastStudyDate(LocalDate lastStudyDate) { this.lastStudyDate = lastStudyDate; }
}

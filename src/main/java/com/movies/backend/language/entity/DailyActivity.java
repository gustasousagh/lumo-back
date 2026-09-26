package com.movies.backend.language.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** XP por dia — alimenta o mapa de atividade e serve de histórico da ofensiva. */
@Entity
@Table(name = "language_daily_activity", indexes = {
        @Index(name = "idx_langactivity_user", columnList = "userId,activity_day", unique = true)
})
public class DailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /**
     * A coluna se chama activity_day porque DAY — assim como DATE — é palavra
     * reservada em SQL: com qualquer um dos dois nomes, a consulta não compila.
     * Mesmo motivo do release_year em Book.
     */
    @Column(name = "activity_day", nullable = false)
    private LocalDate day;

    @Column(nullable = false)
    private int xp = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getDay() { return day; }
    public void setDay(LocalDate day) { this.day = day; }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
}

package com.movies.backend.language.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Uma lição concluída. É o que destrava a próxima na trilha.
 *
 * Refazer uma lição atualiza a linha existente em vez de criar outra: a trilha
 * pergunta "esta lição foi feita?", não "quantas vezes?".
 */
@Entity
@Table(name = "language_lesson_progress", indexes = {
        @Index(name = "idx_langlesson_user", columnList = "userId,lessonId", unique = true)
})
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long lessonId;

    @Column(nullable = false)
    private int correct;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private Instant completedAt = Instant.now();

    /** Quantas vezes a lição foi refeita, para a "coroa" da unidade um dia. */
    @Column(nullable = false)
    private int timesCompleted = 1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
    public int getCorrect() { return correct; }
    public void setCorrect(int correct) { this.correct = correct; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public int getTimesCompleted() { return timesCompleted; }
    public void setTimesCompleted(int timesCompleted) { this.timesCompleted = timesCompleted; }
}

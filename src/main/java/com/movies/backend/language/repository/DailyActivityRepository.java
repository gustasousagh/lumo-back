package com.movies.backend.language.repository;

import com.movies.backend.language.entity.DailyActivity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyActivityRepository extends JpaRepository<DailyActivity, Long> {

    Optional<DailyActivity> findByUserIdAndDay(Long userId, LocalDate day);

    /** Últimos dias, para o mapa de atividade. */
    List<DailyActivity> findByUserIdAndDayGreaterThanEqualOrderByDayAsc(Long userId, LocalDate since);

    void deleteByUserId(Long userId);
}

package com.movies.backend.language.repository;

import com.movies.backend.language.entity.LessonProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    /** Só os ids: é tudo que a trilha precisa para saber o que está concluído. */
    @Query("select p.lessonId from LessonProgress p where p.userId = :userId")
    List<Long> findLessonIdsByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}

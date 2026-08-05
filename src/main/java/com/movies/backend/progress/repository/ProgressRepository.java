package com.movies.backend.progress.repository;

import com.movies.backend.progress.entity.WatchProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressRepository extends JpaRepository<WatchProgress, Long> {

    Optional<WatchProgress> findByUserIdAndContentKey(Long userId, String contentKey);

    List<WatchProgress> findTop30ByUserIdOrderByUpdatedAtDesc(Long userId);
}

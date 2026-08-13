package com.movies.backend.library.repository;

import com.movies.backend.library.entity.BookProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookProgressRepository extends JpaRepository<BookProgress, Long> {

    Optional<BookProgress> findByUserIdAndBookId(Long userId, Long bookId);

    List<BookProgress> findTop12ByUserIdOrderByUpdatedAtDesc(Long userId);

    void deleteByBookId(Long bookId);
}

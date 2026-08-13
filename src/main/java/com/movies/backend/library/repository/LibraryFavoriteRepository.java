package com.movies.backend.library.repository;

import com.movies.backend.library.entity.LibraryFavorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryFavoriteRepository extends JpaRepository<LibraryFavorite, Long> {

    List<LibraryFavorite> findByUserIdAndKindOrderByCreatedAtDesc(Long userId, String kind);

    Optional<LibraryFavorite> findByUserIdAndKindAndRefId(Long userId, String kind, Long refId);

    long countByKindAndRefId(String kind, Long refId);

    void deleteByKindAndRefId(String kind, Long refId);
}

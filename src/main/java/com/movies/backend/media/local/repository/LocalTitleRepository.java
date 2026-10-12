package com.movies.backend.media.local.repository;

import com.movies.backend.media.local.entity.LocalKind;
import com.movies.backend.media.local.entity.LocalTitle;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalTitleRepository extends JpaRepository<LocalTitle, Long> {

    Optional<LocalTitle> findByKindAndSourceId(LocalKind kind, Long sourceId);

    List<LocalTitle> findByKindAndSourceIdIn(LocalKind kind, Collection<Long> sourceIds);

    List<LocalTitle> findByOrderByImportedAtDesc(Pageable page);

    List<LocalTitle> findByKindOrderByImportedAtDesc(LocalKind kind, Pageable page);

    List<LocalTitle> findByTitleContainingIgnoreCaseOrderByImportedAtDesc(String q, Pageable page);

    long countByKind(LocalKind kind);

}

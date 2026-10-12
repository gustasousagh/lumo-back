package com.movies.backend.media.local.repository;

import com.movies.backend.media.local.entity.LocalImportItem;
import com.movies.backend.media.local.entity.LocalImportItem.Status;
import com.movies.backend.media.local.entity.LocalKind;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalImportItemRepository extends JpaRepository<LocalImportItem, Long> {

    boolean existsByKindAndSourceId(LocalKind kind, Long sourceId);

    Optional<LocalImportItem> findFirstByStatusOrderByAttemptsAscIdAsc(Status status);

    long countByStatus(Status status);
}

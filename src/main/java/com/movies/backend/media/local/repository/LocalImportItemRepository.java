package com.movies.backend.media.local.repository;

import com.movies.backend.media.local.entity.LocalImportItem;
import com.movies.backend.media.local.entity.LocalImportItem.Status;
import com.movies.backend.media.local.entity.LocalKind;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface LocalImportItemRepository extends JpaRepository<LocalImportItem, Long> {

    boolean existsByKindAndSourceId(LocalKind kind, Long sourceId);

    Optional<LocalImportItem> findFirstByStatusOrderByAttemptsAscIdAsc(Status status);

    long countByStatus(Status status);

    @Query("select max(i.sourceId) from LocalImportItem i where i.kind = ?1")
    Long maxSourceId(LocalKind kind);

    /** Devolve as falhas para a fila, zerando as tentativas. */
    @Modifying
    @Transactional
    @Query("update LocalImportItem i set i.status = ?2, i.attempts = 0 where i.status = ?1")
    int resetStatus(Status from, Status to);
}

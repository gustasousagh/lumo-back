package com.movies.backend.library.repository;

import com.movies.backend.library.entity.PlayEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayEventRepository extends JpaRepository<PlayEvent, Long> {

    long countByPlayedAtAfter(Instant since);

    void deleteByTrackId(Long trackId);

    /**
     * Instantes dos plays na janela. O agrupamento por dia é feito em Java
     * ({@code AdminService}) de propósito: a alternativa é SQL nativo com
     * date(), que é específico do MySQL e quebra em qualquer outro banco. Para
     * uma janela de 30 dias o volume é pequeno e a portabilidade compensa.
     */
    @Query("select p.playedAt from PlayEvent p where p.playedAt >= :since")
    List<Instant> playedAtSince(@Param("since") Instant since);
}

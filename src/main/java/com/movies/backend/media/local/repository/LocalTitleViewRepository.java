package com.movies.backend.media.local.repository;

import com.movies.backend.media.local.entity.LocalKind;
import com.movies.backend.media.local.entity.LocalTitleView;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LocalTitleViewRepository extends JpaRepository<LocalTitleView, Long> {

    boolean existsByKindAndSourceIdAndUserIdAndViewDay(
            LocalKind kind, Long sourceId, Long userId, LocalDate viewDay);

    /** [kind, sourceId, pessoas distintas] dos mais abertos desde a data. */
    @Query("select v.kind, v.sourceId, count(distinct v.userId) from LocalTitleView v "
            + "where v.viewDay >= ?1 group by v.kind, v.sourceId order by count(distinct v.userId) desc")
    List<Object[]> trendingSince(LocalDate since, Pageable page);
}

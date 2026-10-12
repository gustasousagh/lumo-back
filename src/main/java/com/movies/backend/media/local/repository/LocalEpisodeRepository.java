package com.movies.backend.media.local.repository;

import com.movies.backend.media.local.entity.LocalEpisode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LocalEpisodeRepository extends JpaRepository<LocalEpisode, Long> {

    List<LocalEpisode> findByTitleIdOrderBySeasonNumberAscEpisodeNumberAsc(Long titleId);

    Optional<LocalEpisode> findFirstByTitleIdAndSeasonNumberAndEpisodeNumber(
            Long titleId, int seasonNumber, int episodeNumber);

    Optional<LocalEpisode> findFirstByTitleIdOrderBySeasonNumberAscEpisodeNumberAsc(Long titleId);

    @Modifying
    @Query("delete from LocalEpisode e where e.titleId = ?1")
    void deleteByTitleId(Long titleId);
}

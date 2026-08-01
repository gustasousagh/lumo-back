package com.movies.backend.media.repository;

import com.movies.backend.media.entity.MediaReaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaReactionRepository extends JpaRepository<MediaReaction, Long> {

    List<MediaReaction> findByMediaTypeAndMediaIdAndSeasonNumberAndEpisodeNumber(
            String mediaType, Long mediaId, int seasonNumber, int episodeNumber);

    Optional<MediaReaction> findByUserIdAndMediaTypeAndMediaIdAndSeasonNumberAndEpisodeNumber(
            Long userId, String mediaType, Long mediaId, int seasonNumber, int episodeNumber);
}

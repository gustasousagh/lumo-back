package com.movies.backend.media.repository;

import com.movies.backend.media.entity.MediaComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaCommentRepository extends JpaRepository<MediaComment, Long> {

    List<MediaComment> findByMediaTypeAndMediaIdAndSeasonNumberAndEpisodeNumberOrderByCreatedAtDesc(
            String mediaType, Long mediaId, int seasonNumber, int episodeNumber);
}

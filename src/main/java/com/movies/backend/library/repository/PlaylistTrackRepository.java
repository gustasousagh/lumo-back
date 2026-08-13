package com.movies.backend.library.repository;

import com.movies.backend.library.entity.PlaylistTrack;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Long> {

    List<PlaylistTrack> findByPlaylistIdOrderByPositionAsc(Long playlistId);

    long countByPlaylistId(Long playlistId);

    void deleteByPlaylistId(Long playlistId);

    /** Usado quando o admin apaga uma faixa: some de todas as playlists. */
    void deleteByTrackId(Long trackId);

    @Query("select coalesce(max(pt.position), -1) from PlaylistTrack pt where pt.playlistId = ?1")
    int maxPosition(Long playlistId);
}

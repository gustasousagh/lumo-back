package com.movies.backend.library.repository;

import com.movies.backend.library.entity.PlaylistCollaborator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistCollaboratorRepository extends JpaRepository<PlaylistCollaborator, Long> {

    List<PlaylistCollaborator> findByPlaylistId(Long playlistId);

    List<PlaylistCollaborator> findByUserId(Long userId);

    Optional<PlaylistCollaborator> findByPlaylistIdAndUserId(Long playlistId, Long userId);

    boolean existsByPlaylistIdAndUserId(Long playlistId, Long userId);

    void deleteByPlaylistId(Long playlistId);
}

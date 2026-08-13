package com.movies.backend.library.repository;

import com.movies.backend.library.entity.Playlist;
import com.movies.backend.library.entity.PlaylistVisibility;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    List<Playlist> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    List<Playlist> findByIdInOrderByUpdatedAtDesc(List<Long> ids);

    Optional<Playlist> findByShareCode(String shareCode);

    boolean existsByShareCode(String shareCode);

    List<Playlist> findTop50ByVisibilityOrderByUpdatedAtDesc(PlaylistVisibility visibility);

    long countByVisibility(PlaylistVisibility visibility);
}

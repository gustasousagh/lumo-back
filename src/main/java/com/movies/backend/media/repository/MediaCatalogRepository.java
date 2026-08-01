package com.movies.backend.media.repository;

import com.movies.backend.media.entity.MediaCatalog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaCatalogRepository extends JpaRepository<MediaCatalog, Long> {

    Optional<MediaCatalog> findByMediaTypeAndMediaId(String mediaType, Long mediaId);
}

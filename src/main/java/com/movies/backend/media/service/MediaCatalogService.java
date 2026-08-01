package com.movies.backend.media.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.media.dto.MediaSnapshotRequest;
import com.movies.backend.media.entity.MediaCatalog;
import com.movies.backend.media.repository.MediaCatalogRepository;
import com.movies.backend.media.response.MediaCatalogResponse;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cache do catálogo de mídias. Faz UPSERT por (mediaType, mediaId) para o front
 * empurrar a "foto" uma vez e depois só referenciar por id.
 */
@Service
public class MediaCatalogService {

    private final MediaCatalogRepository catalogRepository;

    public MediaCatalogService(MediaCatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    /** UPSERT da mídia; devolve a entidade persistida (uso interno por outros services). */
    @Transactional
    public MediaCatalog upsert(MediaSnapshotRequest snapshot) {
        MediaCatalog catalog = catalogRepository
                .findByMediaTypeAndMediaId(snapshot.mediaType(), snapshot.mediaId())
                .orElseGet(MediaCatalog::new);

        catalog.setMediaId(snapshot.mediaId());
        catalog.setMediaType(snapshot.mediaType());
        catalog.setContentKind(snapshot.contentKind());
        catalog.setTitle(snapshot.title());
        catalog.setPosterUrl(snapshot.posterUrl());
        catalog.setBackdropUrl(snapshot.backdropUrl());
        catalog.setReleaseDate(snapshot.releaseDate());
        catalog.setVoteAverage(snapshot.voteAverage());
        catalog.setOverview(snapshot.overview());
        catalog.setUpdatedAt(Instant.now());

        return catalogRepository.save(catalog);
    }

    @Transactional
    public MediaCatalogResponse upsertResponse(MediaSnapshotRequest snapshot) {
        return MediaCatalogResponse.from(upsert(snapshot));
    }

    @Transactional(readOnly = true)
    public MediaCatalogResponse get(String mediaType, Long mediaId) {
        return catalogRepository.findByMediaTypeAndMediaId(mediaType, mediaId)
                .map(MediaCatalogResponse::from)
                .orElseThrow(() -> ApiException.notFound("Mídia não encontrada no catálogo"));
    }
}

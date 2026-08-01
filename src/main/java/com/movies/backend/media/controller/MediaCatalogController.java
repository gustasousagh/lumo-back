package com.movies.backend.media.controller;

import com.movies.backend.media.dto.MediaSnapshotRequest;
import com.movies.backend.media.response.MediaCatalogResponse;
import com.movies.backend.media.service.MediaCatalogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cache do catálogo de mídias. */
@RestController
@RequestMapping("/api/media/catalog")
public class MediaCatalogController {

    private final MediaCatalogService catalogService;

    public MediaCatalogController(MediaCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** POST /api/media/catalog -> upsert por (mediaType, mediaId). */
    @PostMapping
    public MediaCatalogResponse upsert(@Valid @RequestBody MediaSnapshotRequest body) {
        return catalogService.upsertResponse(body);
    }

    /** GET /api/media/catalog/{mediaType}/{mediaId} -> foto em cache ou 404. */
    @GetMapping("/{mediaType}/{mediaId}")
    public MediaCatalogResponse get(@PathVariable String mediaType, @PathVariable Long mediaId) {
        return catalogService.get(mediaType, mediaId);
    }
}

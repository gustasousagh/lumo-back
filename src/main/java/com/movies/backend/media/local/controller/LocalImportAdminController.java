package com.movies.backend.media.local.controller;

import com.movies.backend.media.local.entity.LocalImportItem.Status;
import com.movies.backend.media.local.repository.LocalImportItemRepository;
import com.movies.backend.media.local.service.LocalCatalogService;
import com.movies.backend.media.local.service.LocalImporter;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controle da importação local no painel admin (/api/admin/** já exige ADMIN). */
@RestController
@RequestMapping("/api/admin/local-import")
public class LocalImportAdminController {

    private final LocalImporter importer;
    private final LocalCatalogService catalog;
    private final LocalImportItemRepository queue;

    public LocalImportAdminController(
            LocalImporter importer, LocalCatalogService catalog, LocalImportItemRepository queue) {
        this.importer = importer;
        this.catalog = catalog;
        this.queue = queue;
    }

    @GetMapping
    public LocalCatalogService.Status status() {
        return catalog.status();
    }

    @PostMapping("/pause")
    public LocalCatalogService.Status pause() {
        importer.setPaused(true);
        return catalog.status();
    }

    @PostMapping("/resume")
    public LocalCatalogService.Status resume() {
        importer.setPaused(false);
        return catalog.status();
    }

    /** Devolve para a fila os títulos que falharam 3 vezes (ex.: GoCine estava sem vídeo). */
    @PostMapping("/retry-failed")
    public Map<String, Integer> retryFailed() {
        return Map.of("requeued", queue.resetStatus(Status.FAILED, Status.PENDING));
    }
}

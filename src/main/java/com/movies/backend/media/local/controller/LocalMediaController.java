package com.movies.backend.media.local.controller;

import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.HomeFeed;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaVideo;
import com.movies.backend.media.gocine.dto.MediaBrowseResponses.DetailResponse;
import com.movies.backend.media.gocine.dto.MediaBrowseResponses.NoStreamResponse;
import com.movies.backend.media.gocine.dto.MediaBrowseResponses.SearchResponse;
import com.movies.backend.media.gocine.dto.MediaBrowseResponses.StreamOption;
import com.movies.backend.media.gocine.dto.MediaBrowseResponses.StreamResponse;
import com.movies.backend.media.gocine.service.MediaDownloadService;
import com.movies.backend.media.local.service.LocalCatalogService;
import com.movies.backend.security.CurrentUser;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * O catálogo local, com as mesmas rotas e respostas de /api/media — o front só
 * troca o prefixo. Nada aqui chama o GoCine.
 */
@RestController
@RequestMapping("/api/local/media")
public class LocalMediaController {

    private final LocalCatalogService catalog;
    private final MediaDownloadService downloadService;
    private final CurrentUser currentUser;

    public LocalMediaController(
            LocalCatalogService catalog, MediaDownloadService downloadService, CurrentUser currentUser) {
        this.catalog = catalog;
        this.downloadService = downloadService;
        this.currentUser = currentUser;
    }

    @GetMapping("/home")
    public HomeFeed home() {
        return catalog.home();
    }

    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "limit", defaultValue = "30") int limit) {
        return new SearchResponse(catalog.search(q, limit));
    }

    @GetMapping("/detail")
    public DetailResponse detail(
            @RequestParam("type") String type,
            @RequestParam("id") long id,
            @RequestParam(name = "kind", required = false) String kind,
            Authentication auth) {
        MediaType mediaType = requireType(type, id);
        MediaDetail detail = catalog.detail(mediaType, ContentKind.from(kind), id);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ainda não importado");
        }
        catalog.recordView(mediaType, detail.contentKind(), id, currentUser.require(auth).getId());
        return DetailResponse.from(detail);
    }

    @GetMapping("/stream")
    public ResponseEntity<?> stream(
            @RequestParam("type") String type,
            @RequestParam("id") long id,
            @RequestParam(name = "kind", required = false) String kind,
            @RequestParam(name = "season", required = false) Integer season,
            @RequestParam(name = "episode", required = false) Integer episode) {
        List<MediaVideo> videos =
                catalog.streams(requireType(type, id), ContentKind.from(kind), id, season, episode);
        if (videos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(NoStreamResponse.notFound());
        }
        List<StreamOption> streams = videos.stream().map(StreamOption::from).toList();
        StreamOption first = streams.get(0);
        return ResponseEntity.ok(new StreamResponse(streams, first.url(), first.server()));
    }

    /** Mesmo proxy de download de /api/media, mas com o link do banco. */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(
            @RequestParam("type") String type,
            @RequestParam("id") long id,
            @RequestParam(name = "kind", required = false) String kind,
            @RequestParam(name = "season", required = false) Integer season,
            @RequestParam(name = "episode", required = false) Integer episode,
            @RequestParam(name = "server", defaultValue = "0") int server,
            @RequestParam(name = "name", required = false) String name,
            @RequestHeader(name = HttpHeaders.RANGE, required = false) String range) {
        List<MediaVideo> videos =
                catalog.streams(requireType(type, id), ContentKind.from(kind), id, season, episode);
        if (videos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "sem player");
        }
        MediaVideo v = videos.get(server >= 0 && server < videos.size() ? server : 0);
        return downloadService.proxy(v.link(), range, MediaDownloadService.filename(name, season, episode));
    }

    /** Quanto já foi importado — é o que o botão do header mostra. */
    @GetMapping("/status")
    public LocalCatalogService.Status status() {
        return catalog.status();
    }

    private static MediaType requireType(String type, long id) {
        MediaType t = MediaType.from(type);
        if (t == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "parâmetros inválidos");
        }
        return t;
    }
}

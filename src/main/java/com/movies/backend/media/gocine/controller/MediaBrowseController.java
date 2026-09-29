package com.movies.backend.media.gocine.controller;

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
import com.movies.backend.media.gocine.service.GocineService;
import com.movies.backend.media.gocine.service.MediaDownloadService;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Catálogo (GoCine) exposto pela própria API.
 *
 * Exige JWT como qualquer outra rota protegida: antes estes endpoints viviam no
 * Next e eram abertos, o que deixava qualquer um usar o acesso ao GoCine.
 */
@RestController
@RequestMapping("/api/media")
public class MediaBrowseController {

    private final GocineService gocineService;
    private final MediaDownloadService downloadService;

    public MediaBrowseController(GocineService gocineService, MediaDownloadService downloadService) {
        this.gocineService = gocineService;
        this.downloadService = downloadService;
    }

    /** GET /api/media/home -> destaques + seções de carrossel. */
    @GetMapping("/home")
    public HomeFeed home() {
        return gocineService.getHome();
    }

    /** GET /api/media/search?q=matrix -> busca no catálogo. */
    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "limit", defaultValue = "30") int limit) {
        return new SearchResponse(gocineService.search(q, limit));
    }

    /** GET /api/media/detail?type=movie|tv&id=123&kind=series|anime */
    @GetMapping("/detail")
    public DetailResponse detail(
            @RequestParam("type") String type,
            @RequestParam("id") long id,
            @RequestParam(name = "kind", required = false) String kind) {

        if (id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id inválido");
        MediaType mediaType = MediaType.from(type);
        if (mediaType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type inválido");
        }

        MediaDetail detail =
                mediaType == MediaType.MOVIE
                        ? gocineService.getMovieDetail(id)
                        : gocineService.getSeriesDetail(id, ContentKind.from(kind));

        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "não encontrado");
        }
        return DetailResponse.from(detail);
    }

    /** GET /api/media/stream?type=&id=&kind=&season=&episode= -> players disponíveis. */
    @GetMapping("/stream")
    public ResponseEntity<?> stream(
            @RequestParam("type") String type,
            @RequestParam("id") long id,
            @RequestParam(name = "kind", required = false) String kind,
            @RequestParam(name = "season", required = false) Integer season,
            @RequestParam(name = "episode", required = false) Integer episode) {

        MediaType mediaType = MediaType.from(type);
        if (id <= 0 || mediaType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "parâmetros inválidos");
        }

        List<MediaVideo> videos =
                gocineService.resolveStreams(id, mediaType, ContentKind.from(kind), season, episode);

        if (videos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(NoStreamResponse.notFound());
        }

        List<StreamOption> streams = videos.stream().map(StreamOption::from).toList();
        StreamOption first = streams.get(0);
        return ResponseEntity.ok(new StreamResponse(streams, first.url(), first.server()));
    }

    /**
     * GET /api/media/download?type=&id=&kind=&season=&episode=&server=&name=
     *
     * Repassa o arquivo de vídeo com cabeçalho de anexo, para o aparelho poder
     * salvar. Existe porque baixar direto do CDN esbarra em CORS e, no iPhone,
     * em o Safari abrir o vídeo em vez de oferecer salvar.
     *
     * Aceita e repassa `Range`, então download interrompido retoma.
     */
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

        MediaType mediaType = MediaType.from(type);
        if (id <= 0 || mediaType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "parâmetros inválidos");
        }

        String url = downloadService.resolveUrl(
                id, mediaType, ContentKind.from(kind), season, episode, server);
        return downloadService.proxy(url, range, MediaDownloadService.filename(name, season, episode));
    }
}

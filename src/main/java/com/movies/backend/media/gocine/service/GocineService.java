package com.movies.backend.media.gocine.service;

import tools.jackson.databind.JsonNode;
import com.movies.backend.media.gocine.client.GocineClient;
import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.HomeFeed;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaItem;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaVideo;
import com.movies.backend.media.gocine.exception.GocineNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Catálogo do GoCine: home, busca, detalhe e resolução de stream.
 *
 * Antes isto vivia nos route handlers do Next; agora é a API quem fala com o
 * GoCine, então dá pra consumir o catálogo sem passar pelo front.
 */
@Service
public class GocineService {

    private final GocineClient client;
    private final GocineMapper mapper;

    public GocineService(GocineClient client, GocineMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public HomeFeed getHome() {
        return mapper.toHomeFeed(client.get("/media/mobile/default"));
    }

    public List<MediaItem> search(String query, int limit) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) return List.of();
        String path = "/search/" + URLEncoder.encode(q, StandardCharsets.UTF_8) + "/EASYPLEX";
        return mapper.toSearchResults(client.get(path), limit);
    }

    /** Detalhe de filme. Devolve null quando o GoCine não conhece o id. */
    public MediaDetail getMovieDetail(long id) {
        try {
            return mapper.toMovieDetail(client.get("/media/detail/" + id + "/default"), id);
        } catch (GocineNotFoundException e) {
            return null;
        }
    }

    private static String showPath(long id, ContentKind kind) {
        return kind == ContentKind.ANIME
                ? "/animes/show/" + id + "/EASYPLEX"
                : "/series/show/" + id + "/default";
    }

    /**
     * O GoCine separa série e anime em endpoints distintos e nem sempre o cliente
     * sabe qual é. Tenta o preferido primeiro e cai no outro em caso de 404.
     */
    private static List<ContentKind> attempts(ContentKind preferred) {
        return preferred == ContentKind.ANIME
                ? List.of(ContentKind.ANIME, ContentKind.SERIES)
                : List.of(ContentKind.SERIES, ContentKind.ANIME);
    }

    public MediaDetail getSeriesDetail(long id, ContentKind preferredKind) {
        for (ContentKind kind : attempts(preferredKind)) {
            try {
                return mapper.toSeriesDetail(client.get(showPath(id, kind)), id, kind);
            } catch (GocineNotFoundException e) {
                // tenta o próximo tipo
            }
        }
        return null;
    }

    /** Todos os players disponíveis para um filme, ou para um episódio. */
    public List<MediaVideo> resolveStreams(
            long id,
            MediaType mediaType,
            ContentKind contentKind,
            Integer seasonNumber,
            Integer episodeNumber) {

        if (mediaType == MediaType.MOVIE) {
            MediaDetail detail = getMovieDetail(id);
            return detail != null ? detail.videos() : List.of();
        }

        for (ContentKind kind : attempts(contentKind)) {
            try {
                JsonNode payload = client.get(showPath(id, kind));
                List<MediaVideo> videos = mapper.episodeVideos(payload, seasonNumber, episodeNumber);
                if (!videos.isEmpty()) return videos;
            } catch (GocineNotFoundException e) {
                // tenta o próximo tipo
            }
        }
        return List.of();
    }
}

package com.movies.backend.media.gocine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.movies.backend.media.gocine.dto.GocineTypes.CastMember;
import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaItem;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaVideo;
import com.movies.backend.media.gocine.dto.GocineTypes.Season;
import java.util.List;

/** Formatos de resposta dos endpoints de catálogo. */
public final class MediaBrowseResponses {

    private MediaBrowseResponses() {}

    public record SearchResponse(List<MediaItem> results) {}

    /**
     * Detalhe público: mesma coisa que MediaDetail, mas SEM os links de vídeo.
     * Quem quiser assistir pede em /api/media/stream — assim o link não vaza em
     * toda abertura de página.
     */
    public record DetailResponse(
            long id,
            MediaType mediaType,
            @JsonInclude(JsonInclude.Include.NON_NULL) ContentKind contentKind,
            String title,
            String overview,
            String posterUrl,
            String backdropUrl,
            Double voteAverage,
            String releaseDate,
            String genre,
            Integer runtimeMinutes,
            String trailerUrl,
            List<String> genres,
            List<CastMember> cast,
            boolean hasStream,
            List<Season> seasons) {

        public static DetailResponse from(MediaDetail d) {
            return new DetailResponse(
                    d.id(),
                    d.mediaType(),
                    d.contentKind(),
                    d.title(),
                    d.overview(),
                    d.posterUrl(),
                    d.backdropUrl(),
                    d.voteAverage(),
                    d.releaseDate(),
                    d.genre(),
                    d.runtimeMinutes(),
                    d.trailerUrl(),
                    d.genres(),
                    d.cast(),
                    !d.videos().isEmpty(),
                    d.seasons());
        }
    }

    public record StreamOption(String url, String server, String lang, boolean hd) {

        public static StreamOption from(MediaVideo v) {
            return new StreamOption(v.link(), v.server(), v.lang(), v.hd());
        }
    }

    public record StreamResponse(List<StreamOption> streams, String streamUrl, String server) {}

    /** Corpo do 404 de stream — o front distingue "não achei" de erro real. */
    public record NoStreamResponse(List<StreamOption> streams, String streamUrl, String reason) {

        public static NoStreamResponse notFound() {
            return new NoStreamResponse(List.of(), null, "not_found");
        }
    }
}

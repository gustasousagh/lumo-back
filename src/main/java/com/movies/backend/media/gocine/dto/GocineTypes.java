package com.movies.backend.media.gocine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Tipos do catálogo (GoCine). O JSON produzido aqui é o contrato que o front
 * consome — os nomes dos campos são iguais aos da antiga camada TypeScript.
 */
public final class GocineTypes {

    private GocineTypes() {}

    public enum MediaType {
        MOVIE("movie"),
        TV("tv");

        private final String json;

        MediaType(String json) {
            this.json = json;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String json() {
            return json;
        }

        public static MediaType from(String raw) {
            if (raw == null) return null;
            return switch (raw.toLowerCase()) {
                case "movie" -> MOVIE;
                case "tv" -> TV;
                default -> null;
            };
        }
    }

    public enum ContentKind {
        SERIES("series"),
        ANIME("anime");

        private final String json;

        ContentKind(String json) {
            this.json = json;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String json() {
            return json;
        }

        public static ContentKind from(String raw) {
            if (raw == null) return null;
            return switch (raw.toLowerCase()) {
                case "series" -> SERIES;
                case "anime" -> ANIME;
                default -> null;
            };
        }
    }

    /**
     * Item de catálogo. contentKind sai do JSON quando nulo (filmes não têm),
     * exatamente como o `...(t.contentKind ? {...} : {})` do TypeScript fazia.
     */
    public record MediaItem(
            long id,
            MediaType mediaType,
            @JsonInclude(JsonInclude.Include.NON_NULL) ContentKind contentKind,
            String title,
            String overview,
            String posterUrl,
            String backdropUrl,
            Double voteAverage,
            String releaseDate,
            String genre) {}

    public record MediaVideo(String server, String link, String lang, boolean hd) {}

    public record Episode(
            long id,
            int episodeNumber,
            String title,
            String overview,
            String stillUrl,
            Integer runtimeMinutes) {}

    public record Season(int seasonNumber, String name, int episodeCount, List<Episode> episodes) {}

    public record CastMember(String name, String character, String photoUrl) {}

    /** Detalhe completo — inclui os vídeos, que o controller não expõe. */
    public record MediaDetail(
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
            List<MediaVideo> videos,
            List<Season> seasons) {}

    public record HomeSection(String key, String title, List<MediaItem> items) {}

    public record HomeFeed(List<MediaItem> featured, List<HomeSection> sections) {}
}

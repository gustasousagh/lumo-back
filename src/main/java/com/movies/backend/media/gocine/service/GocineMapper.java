package com.movies.backend.media.gocine.service;

import tools.jackson.databind.JsonNode;
import com.movies.backend.media.gocine.dto.GocineTypes.CastMember;
import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.Episode;
import com.movies.backend.media.gocine.dto.GocineTypes.HomeFeed;
import com.movies.backend.media.gocine.dto.GocineTypes.HomeSection;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaItem;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaVideo;
import com.movies.backend.media.gocine.dto.GocineTypes.Season;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Normalização do JSON do GoCine.
 *
 * A API do GoCine é inconsistente: o mesmo dado aparece com nomes diferentes
 * dependendo do endpoint (casterslist/casters/cast, search/data/results), números
 * chegam como string, e o "type" varia entre 'movie'/'Movie'/'filme'/'serie'.
 * Tudo que é defensivo aqui existe por causa disso.
 *
 * Esta classe é pura (JsonNode entra, records saem) para poder ser testada contra
 * payloads reais gravados, sem rede — veja GocineMapperTest.
 */
@Component
public class GocineMapper {

    /* ---------------------------------------------------------------- helpers */

    /** Objeto JSON, ou null se não for objeto (arrays não contam). */
    private static JsonNode asObject(JsonNode v) {
        return v != null && v.isObject() ? v : null;
    }

    private static List<JsonNode> asArray(JsonNode v) {
        if (v == null || !v.isArray()) return List.of();
        List<JsonNode> out = new ArrayList<>(v.size());
        v.forEach(out::add);
        return out;
    }

    /** String não-vazia (já trimada). Só aceita string de verdade, como o TS. */
    private static String str(JsonNode v) {
        if (v == null || !v.isTextual()) return null;
        String s = v.textValue().trim();
        return s.isEmpty() ? null : s;
    }

    /** Número, aceitando string numérica — equivalente ao Number() do JS. */
    private static Double num(JsonNode v) {
        if (v == null) return null;
        if (v.isNumber()) {
            double d = v.doubleValue();
            return Double.isFinite(d) ? d : null;
        }
        if (v.isTextual()) {
            String s = v.textValue().trim();
            if (s.isEmpty()) return 0d; // Number("") === 0
            try {
                double d = Double.parseDouble(s);
                return Double.isFinite(d) ? d : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /** Número estritamente positivo (o `n && n > 0` do TS descarta o zero). */
    private static Double posNum(JsonNode v) {
        Double n = num(v);
        return n != null && n > 0 ? n : null;
    }

    /** Primeiro campo presente e não-nulo — equivale ao `??` encadeado do JS. */
    private static JsonNode nullish(JsonNode r, String... fields) {
        for (String f : fields) {
            JsonNode v = r.get(f);
            if (v != null && !v.isNull()) return v;
        }
        return null;
    }

    private record TypeInfo(MediaType mediaType, ContentKind contentKind) {}

    private static TypeInfo normalizeType(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) {
            case "movie", "movies", "filme", "film" -> new TypeInfo(MediaType.MOVIE, null);
            case "anime", "animes" -> new TypeInfo(MediaType.TV, ContentKind.ANIME);
            case "serie", "series", "tv" -> new TypeInfo(MediaType.TV, ContentKind.SERIES);
            default -> null;
        };
    }

    private static String pickTitle(JsonNode r) {
        String t = str(r.get("title"));
        if (t != null) return t;
        t = str(r.get("name"));
        if (t != null) return t;
        t = str(r.get("original_name"));
        return t != null ? t : "Sem título";
    }

    private static String pickPoster(JsonNode r) {
        String p = str(r.get("poster_path"));
        return p != null ? p : str(r.get("backdrop_path"));
    }

    private static String pickBackdrop(JsonNode r) {
        String b = str(r.get("backdrop_path_tv"));
        if (b != null) return b;
        b = str(r.get("backdrop_path"));
        return b != null ? b : str(r.get("poster_path"));
    }

    /** Tipo declarado no próprio registro, quando não há override. */
    private static String declaredType(JsonNode r) {
        return str(nullish(r, "type", "media_type", "category", "genre"));
    }

    /**
     * Item de catálogo, ou null quando o registro não dá pra aproveitar (sem tipo
     * reconhecível ou sem id).
     *
     * typeStr já vem resolvido pelo chamador: é o equivalente ao spread
     * `{...r, type: X}` do TypeScript. Um valor não-string vira null e derruba o
     * registro, igual ao `str()` do original.
     */
    MediaItem toMediaItem(JsonNode r, String typeStr) {
        TypeInfo t = normalizeType(typeStr);
        if (t == null) return null;

        Double id = posNum(r.get("featured_id"));
        if (id == null) id = posNum(r.get("id"));
        if (id == null) return null;

        Double vote = num(r.get("vote_average"));
        String release = str(r.get("release_date"));
        if (release == null) release = str(r.get("first_air_date"));
        String genre = str(r.get("genre_name"));
        if (genre == null) genre = str(r.get("genre"));

        return new MediaItem(
                id.longValue(),
                t.mediaType(),
                t.contentKind(),
                pickTitle(r),
                str(r.get("overview")) != null ? str(r.get("overview")) : "",
                pickPoster(r),
                pickBackdrop(r),
                vote != null ? Math.round(vote * 10) / 10d : null,
                release,
                genre);
    }

    private static List<MediaItem> dedupe(List<MediaItem> items) {
        Set<String> seen = new HashSet<>();
        List<MediaItem> out = new ArrayList<>(items.size());
        for (MediaItem it : items) {
            if (seen.add(it.mediaType().json() + ":" + it.id())) out.add(it);
        }
        return out;
    }

    /* ------------------------------------------------------------------- home */

    private static final Map<String, String> SECTION_TITLES = new LinkedHashMap<>();

    static {
        SECTION_TITLES.put("trending", "Em alta agora");
        SECTION_TITLES.put("thisweek", "Bombando essa semana");
        SECTION_TITLES.put("popular", "Populares");
        SECTION_TITLES.put("top10", "Top 10 de hoje");
        SECTION_TITLES.put("latest", "Novidades");
        SECTION_TITLES.put("latest_movies", "Filmes recém-chegados");
        SECTION_TITLES.put("recents", "Adicionados recentemente");
        SECTION_TITLES.put("popularSeries", "Séries que todo mundo assiste");
        SECTION_TITLES.put("recommended", "Escolhidos pra você");
        SECTION_TITLES.put("choosed", "Selo da casa");
    }

    private static final List<String> SECTION_ORDER =
            List.of(
                    "trending",
                    "top10",
                    "thisweek",
                    "latest_movies",
                    "popular",
                    "popularSeries",
                    "recommended",
                    "latest",
                    "recents",
                    "choosed");

    private static final Pattern CAMEL = Pattern.compile("([a-z])([A-Z])");
    private static final Pattern SEPARATORS = Pattern.compile("[_-]+");
    private static final Pattern WORD_START = Pattern.compile("\\b\\w");

    static String prettifyKey(String key) {
        String known = SECTION_TITLES.get(key);
        if (known != null) return known;
        String s = CAMEL.matcher(key).replaceAll("$1 $2");
        s = SEPARATORS.matcher(s).replaceAll(" ");
        Matcher m = WORD_START.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, m.group().toUpperCase());
        m.appendTail(sb);
        return sb.toString();
    }

    public HomeFeed toHomeFeed(JsonNode payload) {
        JsonNode root = asObject(payload);
        if (root == null) return new HomeFeed(List.of(), List.of());

        List<MediaItem> featuredRows = new ArrayList<>();
        for (JsonNode n : asArray(root.get("featured"))) {
            JsonNode r = asObject(n);
            if (r == null) continue;
            // o destaque às vezes vem sem "type"; cai pra "genre" e, por fim, "movie"
            JsonNode t = nullish(r, "type", "genre");
            MediaItem item = toMediaItem(r, t == null ? "movie" : str(t));
            if (item != null) featuredRows.add(item);
        }

        List<String> keys = new ArrayList<>();
        for (String k : root.propertyNames()) {
            if (!"featured".equals(k) && root.get(k).isArray()) keys.add(k);
        }
        // ordenação estável: conhecidas primeiro na ordem definida, o resto mantém
        // a ordem em que o GoCine mandou
        keys.sort(
                Comparator.comparingInt(
                        k -> {
                            int i = SECTION_ORDER.indexOf(k);
                            return i == -1 ? 999 : i;
                        }));

        List<HomeSection> sections = new ArrayList<>();
        for (String key : keys) {
            List<MediaItem> items = new ArrayList<>();
            for (JsonNode n : asArray(root.get(key))) {
                JsonNode r = asObject(n);
                if (r == null) continue;
                MediaItem item = toMediaItem(r, declaredType(r));
                if (item != null) items.add(item);
            }
            items = dedupe(items);
            if (!items.isEmpty()) sections.add(new HomeSection(key, prettifyKey(key), items));
        }

        List<MediaItem> featured =
                !featuredRows.isEmpty()
                        ? featuredRows
                        : sections.isEmpty()
                                ? List.of()
                                : sections.get(0).items().stream().limit(5).toList();

        return new HomeFeed(featured, sections);
    }

    /* ------------------------------------------------------------------ busca */

    public List<MediaItem> toSearchResults(JsonNode payload, int limit) {
        JsonNode root = asObject(payload);
        List<JsonNode> rows;
        if (root != null) {
            JsonNode arr = nullish(root, "search", "data", "results");
            if (arr == null) {
                // último recurso: o primeiro array que existir no objeto
                for (JsonNode v : root) {
                    if (v.isArray()) {
                        arr = v;
                        break;
                    }
                }
            }
            rows = asArray(arr);
        } else {
            rows = asArray(payload);
        }
        List<MediaItem> items = new ArrayList<>();
        for (JsonNode n : rows) {
            JsonNode r = asObject(n);
            if (r == null) continue;
            MediaItem item = toMediaItem(r, declaredType(r));
            if (item != null) items.add(item);
        }
        return dedupe(items).stream().limit(limit).toList();
    }

    /* --------------------------------------------------------------- detalhes */

    public List<MediaVideo> toVideos(JsonNode payload) {
        List<MediaVideo> out = new ArrayList<>();
        for (JsonNode n : asArray(payload.get("videos"))) {
            JsonNode v = asObject(n);
            if (v == null) continue;
            String link = str(v.get("link"));
            if (link == null) continue;
            Double hd = num(v.get("hd"));
            out.add(new MediaVideo(str(v.get("server")), link, str(v.get("lang")), hd != null && hd == 1d));
        }
        return out;
    }

    private static List<String> toGenres(JsonNode payload) {
        String namesStr = str(payload.get("genresname"));
        if (namesStr != null) {
            List<String> out = new ArrayList<>();
            for (String s : namesStr.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) out.add(t);
            }
            return out;
        }
        List<String> out = new ArrayList<>();
        for (JsonNode n : asArray(payload.get("genres"))) {
            JsonNode g = asObject(n);
            if (g == null) continue;
            String name = str(g.get("name"));
            if (name != null) out.add(name);
        }
        return out;
    }

    private static List<CastMember> toCast(JsonNode payload) {
        JsonNode raw = nullish(payload, "casterslist", "casters", "cast");
        List<CastMember> out = new ArrayList<>();
        for (JsonNode n : asArray(raw)) {
            JsonNode c = asObject(n);
            if (c == null) continue;
            String name = str(c.get("name"));
            if (name == null) name = str(c.get("original_name"));
            if (name == null) continue; // o TS filtrava o placeholder "—"
            String character = str(c.get("character"));
            if (character == null) character = str(c.get("pivot_character"));
            String photo = str(c.get("profile_path"));
            if (photo == null) photo = str(c.get("photo"));
            out.add(new CastMember(name, character, photo));
            if (out.size() == 20) break;
        }
        return out;
    }

    public MediaDetail toMovieDetail(JsonNode payload, long fallbackId) {
        JsonNode r = asObject(payload);
        if (r == null) r = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        MediaItem base = toMediaItem(r, "movie");
        Double vote = num(r.get("vote_average"));
        Double runtime = posNum(r.get("runtime"));
        return new MediaDetail(
                base != null ? base.id() : fallbackId,
                MediaType.MOVIE,
                null,
                base != null ? base.title() : pickTitle(r),
                base != null ? base.overview() : (str(r.get("overview")) != null ? str(r.get("overview")) : ""),
                base != null ? base.posterUrl() : pickPoster(r),
                base != null ? base.backdropUrl() : pickBackdrop(r),
                base != null ? base.voteAverage() : vote,
                base != null ? base.releaseDate() : str(r.get("release_date")),
                base != null ? base.genre() : null,
                runtime != null ? runtime.intValue() : null,
                str(r.get("trailer_url")),
                toGenres(r),
                toCast(r),
                toVideos(r),
                List.of());
    }

    List<Season> toSeasons(JsonNode payload) {
        List<Season> out = new ArrayList<>();
        for (JsonNode n : asArray(payload.get("seasons"))) {
            JsonNode s = asObject(n);
            if (s == null) continue;
            Double sn = posNum(nullish(s, "season_number", "seasonNumber"));
            int seasonNumber = sn != null ? sn.intValue() : 1;

            List<Episode> episodes = new ArrayList<>();
            List<JsonNode> rawEpisodes = asArray(s.get("episodes"));
            for (int i = 0; i < rawEpisodes.size(); i++) {
                JsonNode e = asObject(rawEpisodes.get(i));
                if (e == null) continue;
                Double eid = posNum(e.get("id"));
                // sem id, o TS montava um sintético concatenando temporada + posição
                long id = eid != null ? eid.longValue() : Long.parseLong("" + seasonNumber + (i + 1));
                Double en = posNum(nullish(e, "episode_number", "episodeNumber", "number"));
                int episodeNumber = en != null ? en.intValue() : i + 1;
                String title = str(e.get("name"));
                if (title == null) title = str(e.get("title"));
                if (title == null) title = "Episódio " + (i + 1);
                String still = str(e.get("still_path_tv"));
                if (still == null) still = str(e.get("still_path"));
                Double runtime = posNum(nullish(e, "runtime", "duration"));
                episodes.add(
                        new Episode(
                                id,
                                episodeNumber,
                                title,
                                str(e.get("overview")) != null ? str(e.get("overview")) : "",
                                still,
                                runtime != null ? runtime.intValue() : null));
            }
            episodes.sort(Comparator.comparingInt(Episode::episodeNumber));

            Double count = posNum(s.get("episode_count"));
            String name = str(s.get("name"));
            out.add(
                    new Season(
                            seasonNumber,
                            name != null ? name : "Temporada " + seasonNumber,
                            count != null ? count.intValue() : episodes.size(),
                            episodes));
        }
        out.sort(Comparator.comparingInt(Season::seasonNumber));
        return out;
    }

    public MediaDetail toSeriesDetail(JsonNode payload, long fallbackId, ContentKind kind) {
        JsonNode r = asObject(payload);
        if (r == null) r = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        MediaItem base = toMediaItem(r, kind.json());
        Double vote = num(r.get("vote_average"));
        Double runtime = posNum(r.get("runtime"));
        String release = str(r.get("first_air_date"));
        if (release == null) release = str(r.get("release_date"));
        return new MediaDetail(
                base != null ? base.id() : fallbackId,
                MediaType.TV,
                kind,
                base != null ? base.title() : pickTitle(r),
                base != null ? base.overview() : (str(r.get("overview")) != null ? str(r.get("overview")) : ""),
                base != null ? base.posterUrl() : pickPoster(r),
                base != null ? base.backdropUrl() : pickBackdrop(r),
                base != null ? base.voteAverage() : vote,
                base != null ? base.releaseDate() : release,
                base != null ? base.genre() : null,
                runtime != null ? runtime.intValue() : null,
                str(r.get("trailer_url")),
                toGenres(r),
                toCast(r),
                toVideos(r),
                toSeasons(r));
    }

    /**
     * Vídeos de um episódio, lidos direto do payload cru: o detalhe público não
     * carrega os links, então o stream precisa reabrir a resposta do GoCine.
     */
    public List<MediaVideo> episodeVideos(JsonNode payload, Integer seasonNumber, Integer episodeNumber) {
        JsonNode r = asObject(payload);
        if (r == null) return List.of();

        List<JsonNode> seasons = new ArrayList<>();
        for (JsonNode n : asArray(r.get("seasons"))) {
            JsonNode s = asObject(n);
            if (s != null) seasons.add(s);
        }

        JsonNode season = null;
        if (seasonNumber != null) {
            for (JsonNode s : seasons) {
                Double sn = posNum(nullish(s, "season_number", "seasonNumber"));
                if (sn != null && sn.intValue() == seasonNumber) {
                    season = s;
                    break;
                }
            }
        }
        if (season == null && !seasons.isEmpty()) season = seasons.get(0);

        List<JsonNode> episodes = new ArrayList<>();
        for (JsonNode n : asArray(season != null ? season.get("episodes") : r.get("episodes"))) {
            JsonNode e = asObject(n);
            if (e != null) episodes.add(e);
        }

        JsonNode episode = null;
        if (episodeNumber != null) {
            for (JsonNode e : episodes) {
                Double en = posNum(nullish(e, "episode_number", "episodeNumber", "number"));
                if (en != null && en.intValue() == episodeNumber) {
                    episode = e;
                    break;
                }
            }
        }
        if (episode == null && !episodes.isEmpty()) episode = episodes.get(0);

        return episode != null ? toVideos(episode) : List.of();
    }
}

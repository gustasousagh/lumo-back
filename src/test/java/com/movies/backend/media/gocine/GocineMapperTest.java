package com.movies.backend.media.gocine;

import static org.assertj.core.api.Assertions.assertThat;

import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.HomeFeed;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaItem;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaVideo;
import com.movies.backend.media.gocine.dto.MediaBrowseResponses.DetailResponse;
import com.movies.backend.media.gocine.dto.MediaBrowseResponses.SearchResponse;
import com.movies.backend.media.gocine.dto.MediaBrowseResponses.StreamOption;
import com.movies.backend.media.gocine.dto.MediaBrowseResponses.StreamResponse;
import com.movies.backend.media.gocine.service.GocineMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prova que o port Java produz exatamente o mesmo JSON que a antiga camada
 * TypeScript produzia.
 *
 * As fixtures em src/test/resources/gocine são reais:
 *   raw-*.json   respostas cruas do GoCine
 *   gold-*.json  saída das rotas /api/media/* do Next, gravada antes da migração
 *
 * Se o mapper divergir do comportamento original, estes testes quebram.
 */
class GocineMapperTest {

    private final ObjectMapper json = JsonMapper.builder().build();
    private final GocineMapper mapper = new GocineMapper();

    private JsonNode fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/gocine/" + name)) {
            assertThat(in).as("fixture %s deve existir", name).isNotNull();
            return json.readTree(in);
        }
    }

    /** Serializa o objeto e devolve como árvore, para comparar campo a campo. */
    private JsonNode tree(Object value) {
        return json.valueToTree(value);
    }

    /**
     * Compara duas árvores tratando números iguais como iguais mesmo quando o tipo
     * difere: o JSON do Node escreve 9 onde o Java escreve 9.0, e para qualquer
     * cliente JavaScript os dois são o mesmo number.
     */
    private static void assertJsonEquals(JsonNode expected, JsonNode actual, String path) {
        if (expected.isNumber() && actual.isNumber()) {
            assertThat(actual.doubleValue()).as("número em %s", path).isEqualTo(expected.doubleValue());
            return;
        }
        if (expected.isObject() && actual.isObject()) {
            assertThat(actual.propertyNames())
                    .as("mesmas chaves em %s", path)
                    .containsExactlyInAnyOrderElementsOf(expected.propertyNames());
            for (String key : expected.propertyNames()) {
                assertJsonEquals(expected.get(key), actual.get(key), path + "." + key);
            }
            return;
        }
        if (expected.isArray() && actual.isArray()) {
            assertThat(actual.size()).as("tamanho de %s", path).isEqualTo(expected.size());
            for (int i = 0; i < expected.size(); i++) {
                assertJsonEquals(expected.get(i), actual.get(i), path + "[" + i + "]");
            }
            return;
        }
        assertThat(actual).as("valor em %s", path).isEqualTo(expected);
    }

    @Test
    @DisplayName("home: destaques e seções idênticos à saída do Next")
    void homeMatchesGolden() throws IOException {
        HomeFeed feed = mapper.toHomeFeed(fixture("raw-home.json"));

        // A rota antiga do Next devolvia também um "soon" com mocks de música e
        // livros (mock-media.ts). Isso não vem do GoCine e continua sendo coisa do
        // front, então a API de catálogo não reproduz esse campo de propósito.
        JsonNode expected = fixture("gold-home.json");
        assertThat(expected.has("soon")).as("a golden realmente tinha o campo mock").isTrue();
        ((tools.jackson.databind.node.ObjectNode) expected).remove("soon");

        assertJsonEquals(expected, tree(feed), "home");
    }

    @Test
    @DisplayName("home: seções na ordem editorial, não na ordem do GoCine")
    void homeOrdersSections() throws IOException {
        HomeFeed feed = mapper.toHomeFeed(fixture("raw-home.json"));
        assertThat(feed.sections().stream().map(s -> s.key()).toList())
                .containsExactly(
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
        // o destaque usa featured_id, não id — foi assim que o TypeScript fez
        assertThat(feed.featured().get(0).id()).isEqualTo(28100L);
    }

    @Test
    @DisplayName("busca: resultados idênticos à saída do Next")
    void searchMatchesGolden() throws IOException {
        List<MediaItem> results = mapper.toSearchResults(fixture("raw-search.json"), 30);
        assertJsonEquals(fixture("gold-search.json"), tree(new SearchResponse(results)), "search");
    }

    @Test
    @DisplayName("detalhe de filme: idêntico, e sem vazar os links de vídeo")
    void movieDetailMatchesGolden() throws IOException {
        MediaDetail detail = mapper.toMovieDetail(fixture("raw-movie.json"), 3L);
        JsonNode actual = tree(DetailResponse.from(detail));

        assertJsonEquals(fixture("gold-movie.json"), actual, "movie");
        assertThat(actual.has("videos")).as("o detalhe não expõe links").isFalse();
        assertThat(actual.get("hasStream").booleanValue()).isTrue();
    }

    @Test
    @DisplayName("detalhe de série: temporadas e episódios idênticos")
    void seriesDetailMatchesGolden() throws IOException {
        MediaDetail detail = mapper.toSeriesDetail(fixture("raw-series.json"), 3556L, ContentKind.SERIES);
        assertJsonEquals(fixture("gold-series.json"), tree(DetailResponse.from(detail)), "series");
    }

    @Test
    @DisplayName("stream de filme: mesmos players, na mesma ordem")
    void movieStreamsMatchGolden() throws IOException {
        MediaDetail detail = mapper.toMovieDetail(fixture("raw-movie.json"), 3L);
        List<StreamOption> streams = detail.videos().stream().map(StreamOption::from).toList();
        StreamResponse response =
                new StreamResponse(streams, streams.get(0).url(), streams.get(0).server());

        assertJsonEquals(fixture("gold-stream-movie.json"), tree(response), "stream");
    }

    @Test
    @DisplayName("stream de episódio: resolve temporada 1 / episódio 1")
    void episodeStreamsMatchGolden() throws IOException {
        List<MediaVideo> videos = mapper.episodeVideos(fixture("raw-series.json"), 1, 1);
        List<StreamOption> streams = videos.stream().map(StreamOption::from).toList();
        StreamResponse response =
                new StreamResponse(streams, streams.get(0).url(), streams.get(0).server());

        assertJsonEquals(fixture("gold-stream-ep.json"), tree(response), "streamEp");
    }

    @Test
    @DisplayName("filme não tem contentKind no JSON; série tem")
    void contentKindOnlyOnSeries() throws IOException {
        JsonNode movie = tree(DetailResponse.from(mapper.toMovieDetail(fixture("raw-movie.json"), 3L)));
        assertThat(movie.has("contentKind")).isFalse();

        JsonNode series =
                tree(
                        DetailResponse.from(
                                mapper.toSeriesDetail(fixture("raw-series.json"), 3556L, ContentKind.SERIES)));
        assertThat(series.get("contentKind").stringValue()).isEqualTo("series");
    }
}

package com.movies.backend.media.gocine.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.media.gocine.dto.GocineTypes.ContentKind;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaType;
import com.movies.backend.media.gocine.dto.GocineTypes.MediaVideo;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Repassa o arquivo de vídeo do CDN para o cliente.
 *
 * POR QUE ISTO EXISTE
 *
 * O link do GoCine aponta para um CDN de terceiro que redireciona e valida um
 * token de vida curta. Baixar direto do navegador esbarra em duas coisas:
 *
 *   1. CORS. O CDN pode não permitir leitura por JavaScript, e sem isso não dá
 *      para acompanhar progresso nem guardar o arquivo no aparelho.
 *   2. Content-Disposition. Sem ele o Safari abre o vídeo no player nativo em
 *      vez de oferecer salvar — no iPhone é a diferença entre baixar e não.
 *
 * Passando por aqui os dois somem: a resposta sai da nossa origem, com CORS já
 * configurado, e com o cabeçalho de anexo.
 *
 * O preço é a banda de upload do servidor. Por isso o front tenta o CDN direto
 * primeiro e só cai para cá quando aquele caminho falha.
 */
@Service
public class MediaDownloadService {

    /**
     * Sem timeout de resposta: um filme de 2 GB numa conexão ruim leva muito
     * mais que qualquer limite razoável, e cortar no meio seria pior que
     * demorar. O timeout de CONEXÃO continua, que é o que pega servidor morto.
     */
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    /**
     * O CDN responde diferente conforme o User-Agent em alguns casos. Mandamos
     * um de navegador para o comportamento ser o mesmo que o do aparelho.
     */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 "
                    + "(KHTML, like Gecko) Version/17.0 Safari/605.1.15";

    private final GocineService gocineService;

    public MediaDownloadService(GocineService gocineService) {
        this.gocineService = gocineService;
    }

    /** Descobre a URL do arquivo, já resolvida com token novo. */
    public String resolveUrl(long id, MediaType type, ContentKind kind,
                             Integer season, Integer episode, int serverIndex) {
        List<MediaVideo> videos = gocineService.resolveStreams(id, type, kind, season, episode);
        if (videos.isEmpty()) {
            throw ApiException.notFound("Nenhum servidor disponível para este título.");
        }
        if (serverIndex < 0 || serverIndex >= videos.size()) {
            serverIndex = 0;
        }
        String url = videos.get(serverIndex).link();
        if (url == null || url.isBlank()) {
            throw ApiException.notFound("O servidor escolhido não devolveu um arquivo.");
        }
        return url;
    }

    /**
     * Abre o arquivo no CDN e devolve a resposta pronta para repassar.
     *
     * O `Range` do cliente vai adiante sem alteração, e o status do CDN é
     * espelhado: assim um download interrompido retoma de onde parou em vez de
     * recomeçar, que num arquivo de 2 GB é a diferença entre incômodo e
     * desistência.
     */
    public ResponseEntity<InputStreamResource> proxy(String sourceUrl, String range, String filename) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(sourceUrl))
                .header("User-Agent", USER_AGENT)
                .GET();
        if (range != null && !range.isBlank()) {
            request.header(HttpHeaders.RANGE, range);
        }

        HttpResponse<InputStream> upstream;
        try {
            upstream = http.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível alcançar o servidor do vídeo.");
        }

        int status = upstream.statusCode();
        if (status >= 400) {
            // 403 aqui quase sempre é token expirado — o front deve pedir de novo.
            throw new ApiException(
                    status == HttpStatus.FORBIDDEN.value() ? HttpStatus.GONE : HttpStatus.BAD_GATEWAY,
                    status == HttpStatus.FORBIDDEN.value()
                            ? "O link do vídeo expirou. Tente baixar de novo."
                            : "O servidor do vídeo respondeu " + status + ".");
        }

        HttpHeaders headers = new HttpHeaders();
        copyHeader(upstream, headers, HttpHeaders.CONTENT_TYPE);
        copyHeader(upstream, headers, HttpHeaders.CONTENT_LENGTH);
        copyHeader(upstream, headers, HttpHeaders.CONTENT_RANGE);

        if (headers.getContentType() == null) {
            headers.setContentType(MediaTypeFactory.getMediaType(filename)
                    .orElse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM));
        }
        // Sempre anunciamos Range: é o que permite retomar, e o CDN aceita.
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename, java.nio.charset.StandardCharsets.UTF_8).build());
        // O arquivo é imutável enquanto o link durar, mas o link é curto: não
        // vale guardar em cache intermediário.
        headers.setCacheControl("private, no-store");

        return ResponseEntity.status(status)
                .headers(headers)
                .body(new InputStreamResource(upstream.body()));
    }

    private static void copyHeader(HttpResponse<?> from, HttpHeaders to, String name) {
        Optional<String> value = from.headers().firstValue(name);
        value.ifPresent(v -> to.set(name, v));
    }

    /**
     * Nome do arquivo salvo no aparelho.
     *
     * Barra e dois-pontos quebram o nome em parte dos sistemas, e aspas quebram
     * o próprio cabeçalho; o resto é cosmético.
     */
    public static String filename(String title, Integer season, Integer episode) {
        String base = (title == null || title.isBlank()) ? "video" : title.trim();
        base = base.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "-").replaceAll("\\s+", " ").trim();
        if (base.length() > 80) base = base.substring(0, 80).trim();
        if (season != null && episode != null) {
            base += String.format(" S%02dE%02d", season, episode);
        }
        return base + ".mp4";
    }
}

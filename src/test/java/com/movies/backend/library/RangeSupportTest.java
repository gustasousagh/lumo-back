package com.movies.backend.library;

import static org.assertj.core.api.Assertions.assertThat;

import com.movies.backend.library.storage.RangeSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * O Range é o que faz o player conseguir arrastar a barra e o leitor de PDF
 * abrir na página 300 sem baixar o livro inteiro. Errar o cálculo do trecho não
 * dá erro visível: o áudio simplesmente começa no lugar errado ou corta. Por
 * isso os limites estão presos aqui.
 */
class RangeSupportTest {

    @TempDir static Path tempDir;

    private static Path file;
    /** 26 bytes, um por letra — assim o trecho pedido é conferível a olho. */
    private static final String CONTENT = "abcdefghijklmnopqrstuvwxyz";

    @BeforeAll
    static void createFile() throws Exception {
        file = tempDir.resolve("faixa.mp3");
        Files.writeString(file, CONTENT, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("sem Range devolve 200 com o arquivo inteiro")
    void fullResponse() throws Exception {
        var response = serve(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(26);
        // Sem isto o navegador nem tenta pedir um trecho.
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        assertThat(body(response)).isEqualTo(CONTENT);
    }

    @Test
    @DisplayName("intervalo fechado devolve 206 só com os bytes pedidos")
    void closedRange() throws Exception {
        var response = serve("bytes=3-7");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 3-7/26");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(5);
        assertThat(body(response)).isEqualTo("defgh");
    }

    @Test
    @DisplayName("intervalo aberto vai até o fim do arquivo")
    void openEndedRange() throws Exception {
        // É a forma que o <audio> usa ao dar seek: "me dá daqui em diante".
        var response = serve("bytes=20-");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 20-25/26");
        assertThat(body(response)).isEqualTo("uvwxyz");
    }

    @Test
    @DisplayName("sufixo devolve os últimos N bytes")
    void suffixRange() throws Exception {
        // Alguns players pedem o rodapé do arquivo primeiro (tags no fim).
        var response = serve("bytes=-4");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 22-25/26");
        assertThat(body(response)).isEqualTo("wxyz");
    }

    @Test
    @DisplayName("fim depois do tamanho do arquivo é cortado, não recusado")
    void rangeBeyondEndIsClamped() throws Exception {
        var response = serve("bytes=24-999");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 24-25/26");
        assertThat(body(response)).isEqualTo("yz");
    }

    @Test
    @DisplayName("início fora do arquivo devolve 416")
    void unsatisfiableRange() throws Exception {
        var response = serve("bytes=100-200");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes */26");
    }

    @Test
    @DisplayName("Range malformado cai para a resposta inteira, sem quebrar")
    void malformedRangeFallsBack() throws Exception {
        // Um header estranho não pode derrubar a reprodução: melhor mandar tudo.
        assertThat(serve("bytes=abc").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(serve("items=0-5").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("download nomeado sai como anexo; stream sai inline")
    void dispositionDependsOnDownloadName() {
        var attachment = RangeSupport.serve(request(null), file, "audio/mpeg", "Minha Música.mp3");
        assertThat(attachment.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment");

        var inline = RangeSupport.serve(request(null), file, "audio/mpeg", null);
        assertThat(inline.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("inline");
    }

    // ---------------------------------------------------------------- HELPERS
    private static ResponseEntity<Resource> serve(String rangeHeader) {
        return RangeSupport.serve(request(rangeHeader), file, "audio/mpeg", null);
    }

    private static MockHttpServletRequest request(String rangeHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (rangeHeader != null) {
            request.addHeader(HttpHeaders.RANGE, rangeHeader);
        }
        return request;
    }

    private static String body(ResponseEntity<Resource> response) throws Exception {
        try (var in = response.getBody().getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

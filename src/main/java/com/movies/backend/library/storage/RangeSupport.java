package com.movies.backend.library.storage;

import com.movies.backend.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Serve arquivos do disco respondendo ao header {@code Range}.
 *
 * <p>Isso não é enfeite: sem 206/Partial Content o {@code <audio>} não consegue
 * arrastar a barra de progresso (ele precisa pedir "me dá a partir do byte X"), o
 * navegador baixa a faixa inteira antes de tocar, e o visualizador de PDF não
 * abre a página 300 de um livro de 40MB sem baixar tudo. O Safari em particular
 * se recusa a tocar áudio de um servidor que ignora Range.
 *
 * <p>A leitura é em streaming ({@link InputStreamResource}): carregar o arquivo
 * inteiro em memória para responder derrubaria o servidor com poucos ouvintes
 * simultâneos.
 */
public final class RangeSupport {

    private RangeSupport() {
    }

    /**
     * Monta a resposta do arquivo, honrando Range quando o cliente pedir.
     *
     * @param downloadName nome que aparece ao salvar; {@code null} = tocar/exibir
     *                     no próprio navegador em vez de baixar
     */
    public static ResponseEntity<Resource> serve(HttpServletRequest request,
                                                 Path file,
                                                 String contentType,
                                                 String downloadName) {
        if (!Files.isReadable(file)) {
            throw ApiException.notFound("Arquivo não encontrado no servidor");
        }

        long length;
        try {
            length = Files.size(file);
        } catch (IOException e) {
            throw ApiException.notFound("Arquivo não encontrado no servidor");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        // Sem isto o navegador nem tenta pedir um trecho.
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.setContentDisposition(disposition(downloadName));
        // O conteúdo nunca muda (o nome no disco é um UUID), então cache longo.
        headers.setCacheControl("private, max-age=31536000, immutable");

        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            headers.setContentLength(length);
            return new ResponseEntity<>(stream(file, 0, length), headers, HttpStatus.OK);
        }

        long[] range = parseRange(rangeHeader, length);
        if (range == UNPARSEABLE) {
            // Header que não dá para entender é ignorado, e a resposta sai
            // inteira (RFC 9110 §14.2). Devolver 416 aqui faria um cliente com
            // header esquisito não conseguir tocar nada, em vez de tocar do
            // começo.
            headers.setContentLength(length);
            return new ResponseEntity<>(stream(file, 0, length), headers, HttpStatus.OK);
        }
        if (range == null) {
            // Bem-formado, mas fora do arquivo: aí sim é 416.
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes */" + length);
            return new ResponseEntity<>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        long start = range[0];
        long end = range[1];
        long count = end - start + 1;
        headers.setContentLength(count);
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + length);
        return new ResponseEntity<>(stream(file, start, count), headers, HttpStatus.PARTIAL_CONTENT);
    }

    /** Marcador para "não consegui nem entender o header". */
    private static final long[] UNPARSEABLE = new long[0];

    /**
     * Interpreta "bytes=0-1023", "bytes=500-" e "bytes=-500" (últimos 500).
     *
     * <p>Três saídas diferentes, e a distinção importa:
     * {@link #UNPARSEABLE} = header sem sentido (o chamador ignora e manda tudo);
     * {@code null} = sintaxe válida mas o intervalo não existe no arquivo (416);
     * um par {início, fim} = o trecho a servir (206).
     */
    private static long[] parseRange(String header, long length) {
        // Só o primeiro trecho: múltiplos ranges exigiriam resposta multipart, que
        // nenhum player de áudio usa na prática.
        String spec = header.substring("bytes=".length()).split(",")[0].trim();
        int dash = spec.indexOf('-');
        if (dash < 0) {
            return UNPARSEABLE;
        }

        String rawStart = spec.substring(0, dash).trim();
        String rawEnd = spec.substring(dash + 1).trim();

        try {
            long start;
            long end;
            if (rawStart.isEmpty()) {
                // sufixo: os últimos N bytes
                long suffix = Long.parseLong(rawEnd);
                if (suffix <= 0) {
                    return UNPARSEABLE;
                }
                start = Math.max(0, length - suffix);
                end = length - 1;
            } else {
                start = Long.parseLong(rawStart);
                end = rawEnd.isEmpty() ? length - 1 : Long.parseLong(rawEnd);
            }

            if (start < 0 || start >= length || end < start) {
                return null;
            }
            return new long[]{start, Math.min(end, length - 1)};
        } catch (NumberFormatException ex) {
            return UNPARSEABLE;
        }
    }

    /** Recurso que lê só a fatia pedida, sem trazer o arquivo para a memória. */
    private static Resource stream(Path file, long start, long count) {
        return new InputStreamResource(new LimitedFileStream(file, start, count));
    }

    private static ContentDisposition disposition(String downloadName) {
        if (downloadName == null) {
            return ContentDisposition.inline().build();
        }
        // O nome pode ter acento/espaço: filename* em UTF-8 é o que preserva.
        return ContentDisposition.attachment()
                .filename(sanitize(downloadName), StandardCharsets.UTF_8)
                .build();
    }

    /** Tira o que quebraria o header Content-Disposition. */
    public static String sanitize(String name) {
        String cleaned = name.replaceAll("[\\r\\n\"\\\\/]", "_").trim();
        return cleaned.isEmpty() ? "arquivo" : cleaned;
    }

    /** Só para montar URLs; não usado no header. */
    public static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * InputStream que começa em {@code start} e entrega no máximo {@code count}
     * bytes. Existe porque {@code Files.newInputStream} não sabe pular direto
     * para um offset sem ler o miolo, e porque precisamos parar no fim do range.
     */
    private static final class LimitedFileStream extends InputStream {

        private final InputStream delegate;
        private long remaining;

        private LimitedFileStream(Path file, long start, long count) {
            try {
                this.delegate = Files.newInputStream(file);
                long skipped = 0;
                while (skipped < start) {
                    long step = delegate.skip(start - skipped);
                    if (step <= 0) {
                        break;
                    }
                    skipped += step;
                }
                this.remaining = count;
            } catch (IOException e) {
                throw ApiException.notFound("Não foi possível ler o arquivo");
            }
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = delegate.read();
            if (value >= 0) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int toRead = (int) Math.min(len, remaining);
            int read = delegate.read(buffer, off, toRead);
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    /** Copia um arquivo inteiro para um OutputStream (usado no ZIP). */
    public static void copyTo(Path file, OutputStream out) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            in.transferTo(out);
        }
    }
}

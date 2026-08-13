package com.movies.backend.library.metadata;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lê as tags ID3 de um MP3: título, artista, álbum, ano, faixa, duração e a capa
 * embutida. É isso que faz o upload virar uma faixa completa sem o admin digitar
 * nada — e é isso que a busca por artista/álbum consulta depois.
 *
 * <p>Estratégia de tolerância: nada aqui lança exceção para fora. Um MP3 com tag
 * corrompida, um WAV renomeado para .mp3 ou um arquivo sem tag nenhuma devolvem
 * {@link AudioMetadata#empty()} e o cadastro segue com o nome do arquivo como
 * título. Recusar o upload por causa de metadado seria pior que aceitar com o
 * campo vazio: o áudio toca do mesmo jeito.
 */
@Component
public class AudioMetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(AudioMetadataExtractor.class);

    public AudioMetadata extract(Path file) {
        try {
            Mp3File mp3 = new Mp3File(file.toFile());
            Integer duration = (int) mp3.getLengthInSeconds();
            Integer bitrate = mp3.getBitrate();

            if (mp3.hasId3v2Tag()) {
                return fromV2(mp3.getId3v2Tag(), duration, bitrate);
            }
            if (mp3.hasId3v1Tag()) {
                return fromV1(mp3.getId3v1Tag(), duration, bitrate);
            }
            // Sem tag, mas ainda conseguimos duração e bitrate do próprio stream.
            return new AudioMetadata(null, null, null, null, null, null, null, null,
                    duration, bitrate, null, null);
        } catch (Exception ex) {
            log.debug("Não deu para ler as tags de {}: {}", file.getFileName(), ex.toString());
            return AudioMetadata.empty();
        }
    }

    private AudioMetadata fromV2(ID3v2 tag, Integer duration, Integer bitrate) {
        byte[] cover = null;
        String coverMime = null;
        try {
            cover = tag.getAlbumImage();
            coverMime = tag.getAlbumImageMimeType();
        } catch (Exception ignored) {
            // capa corrompida: a faixa entra sem capa
        }
        return new AudioMetadata(
                clean(tag.getTitle()),
                clean(tag.getArtist()),
                clean(tag.getAlbum()),
                clean(tag.getAlbumArtist()),
                cleanGenre(safeGenre(tag)),
                parseYear(tag.getYear()),
                parseSlashed(tag.getTrack()),
                parseSlashed(tag.getPartOfSet()),
                duration,
                bitrate,
                cover != null && cover.length > 0 ? cover : null,
                normalizeMime(coverMime));
    }

    private AudioMetadata fromV1(ID3v1 tag, Integer duration, Integer bitrate) {
        return new AudioMetadata(
                clean(tag.getTitle()),
                clean(tag.getArtist()),
                clean(tag.getAlbum()),
                null,
                cleanGenre(safeGenre(tag)),
                parseYear(tag.getYear()),
                parseSlashed(tag.getTrack()),
                null,
                duration,
                bitrate,
                null,
                null);
    }

    /** getGenreDescription explode em alguns arquivos com índice de gênero fora da tabela. */
    private String safeGenre(ID3v1 tag) {
        try {
            return tag.getGenreDescription();
        } catch (Exception ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------ PARSE
    private String clean(String value) {
        if (value == null) {
            return null;
        }
        // Tags vindas de tocadores antigos costumam trazer NUL de padding.
        String trimmed = value.replace("\u0000", "").trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Alguns arquivos trazem o gênero como "(17)Rock" — fica só "Rock". */
    private String cleanGenre(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        String withoutIndex = cleaned.replaceFirst("^\\(\\d+\\)\\s*", "").trim();
        return withoutIndex.isEmpty() ? cleaned : withoutIndex;
    }

    /** O ano pode vir "1997", "1997-05-12" ou lixo; só interessa o ano. */
    private Integer parseYear(String value) {
        String cleaned = clean(value);
        if (cleaned == null || cleaned.length() < 4) {
            return null;
        }
        try {
            int year = Integer.parseInt(cleaned.substring(0, 4));
            return (year > 1400 && year < 2200) ? year : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Faixa e disco vêm como "3" ou "3/12" — queremos o 3. */
    private Integer parseSlashed(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        String head = cleaned.split("/")[0].trim();
        try {
            int parsed = Integer.parseInt(head);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeMime(String mime) {
        if (mime == null || mime.isBlank()) {
            return "image/jpeg";
        }
        String lower = mime.toLowerCase();
        // ID3 antigo usa só "PNG"/"JPG" no lugar do mime completo.
        if (lower.contains("png")) {
            return "image/png";
        }
        return "image/jpeg";
    }
}

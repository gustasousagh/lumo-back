package com.movies.backend.library.metadata;

/**
 * O que conseguimos ler de dentro de um arquivo de áudio. Todo campo pode vir
 * nulo — arquivo sem tag nenhuma é comum, e isso não impede o cadastro.
 *
 * @param coverImage bytes da capa embutida, quando existe
 * @param coverMime  mime da capa ("image/jpeg", "image/png")
 */
public record AudioMetadata(
        String title,
        String artist,
        String album,
        String albumArtist,
        String genre,
        Integer year,
        Integer trackNumber,
        Integer discNumber,
        Integer durationSec,
        Integer bitrateKbps,
        byte[] coverImage,
        String coverMime
) {
    public static AudioMetadata empty() {
        return new AudioMetadata(null, null, null, null, null, null, null, null, null, null, null, null);
    }
}

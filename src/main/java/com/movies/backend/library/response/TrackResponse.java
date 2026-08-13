package com.movies.backend.library.response;

import com.movies.backend.library.entity.Track;

/**
 * Faixa como o front recebe.
 *
 * <p>Não devolvemos {@code storageKey} nem {@code checksum}: são detalhes de
 * onde o arquivo mora no disco, e o cliente monta as URLs de stream/download a
 * partir do id. Assim dá para trocar o backend de armazenamento sem mexer no
 * front.
 */
public record TrackResponse(
        Long id,
        String title,
        String artist,
        String album,
        String albumArtist,
        String genre,
        Integer year,
        Integer trackNumber,
        Integer discNumber,
        Integer durationSec,
        String coverUrl,
        long fileSize,
        long playCount,
        boolean favorite
) {
    public static TrackResponse from(Track track) {
        return from(track, false);
    }

    public static TrackResponse from(Track track, boolean favorite) {
        return new TrackResponse(
                track.getId(),
                track.getTitle(),
                track.getArtist(),
                track.getAlbum(),
                track.getAlbumArtist(),
                track.getGenre(),
                track.getYear(),
                track.getTrackNumber(),
                track.getDiscNumber(),
                track.getDurationSec(),
                track.getCoverUrl(),
                track.getFileSize(),
                track.getPlayCount(),
                favorite);
    }
}

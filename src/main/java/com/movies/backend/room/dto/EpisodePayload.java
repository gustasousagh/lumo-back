package com.movies.backend.room.dto;

/** Payload do WS /app/room/{id}/episode: troca de temporada/episódio pelo host. */
public record EpisodePayload(Integer seasonNumber, Integer episodeNumber) {
}

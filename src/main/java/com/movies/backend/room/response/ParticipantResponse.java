package com.movies.backend.room.response;

import com.movies.backend.user.response.UserMiniResponse;
import java.time.Instant;

/** Um participante da sala com seu papel. */
public record ParticipantResponse(
        UserMiniResponse user,
        String role,
        Instant joinedAt
) {
}

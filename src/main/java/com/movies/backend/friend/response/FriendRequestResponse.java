package com.movies.backend.friend.response;

import com.movies.backend.user.response.UserMiniResponse;
import java.time.Instant;

/**
 * Um convite de amizade na listagem. "user" é o OUTRO usuário envolvido
 * (o remetente na caixa de entrada; o destinatário na caixa de saída).
 */
public record FriendRequestResponse(
        Long id,
        String status,
        UserMiniResponse user,
        Instant createdAt
) {
}

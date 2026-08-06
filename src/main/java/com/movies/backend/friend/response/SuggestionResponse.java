package com.movies.backend.friend.response;

import com.movies.backend.user.response.UserMiniResponse;

/** Sugestão de amizade: mini perfil + quantos amigos em comum. */
public record SuggestionResponse(UserMiniResponse user, int mutualFriends) {
}

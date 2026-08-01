package com.movies.backend.security;

import com.movies.backend.exception.ApiException;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Resolve o usuário logado a partir do {@link Authentication}.
 * O "name" da autenticação é sempre o email (subject do JWT).
 * Centraliza a checagem para os controllers não repetirem esse código.
 */
@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Carrega o usuário logado ou lança 401 se não houver autenticação válida. */
    public User require(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw ApiException.unauthorized("Não autenticado");
        }
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> ApiException.unauthorized("Não autenticado"));
    }
}

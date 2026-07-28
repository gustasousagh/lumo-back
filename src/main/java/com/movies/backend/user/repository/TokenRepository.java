package com.movies.backend.user.repository;

import com.movies.backend.user.entity.Token;
import com.movies.backend.user.entity.TokenType;
import com.movies.backend.user.entity.User;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByValueAndType(String value, TokenType type);

    /** Remove tokens antigos do mesmo tipo antes de gerar um novo. */
    @Transactional
    void deleteByUserAndType(User user, TokenType type);
}

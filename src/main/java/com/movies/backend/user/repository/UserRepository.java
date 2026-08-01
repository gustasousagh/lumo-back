package com.movies.backend.user.repository;

import com.movies.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository = camada de acesso ao banco. O Spring Data cria a implementação
 * sozinho a partir do nome dos métodos. O Service chama esses métodos.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** Busca por apelido OU nome (usada na barra de busca de usuários). */
    List<User> findTop20ByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(String a, String b);
}
